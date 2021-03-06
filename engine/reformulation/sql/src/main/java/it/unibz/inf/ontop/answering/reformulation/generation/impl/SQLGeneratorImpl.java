package it.unibz.inf.ontop.answering.reformulation.generation.impl;


import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import it.unibz.inf.ontop.answering.reformulation.generation.IQTree2NativeNodeGenerator;
import it.unibz.inf.ontop.answering.reformulation.generation.NativeQueryGenerator;
import it.unibz.inf.ontop.answering.reformulation.generation.PostProcessingProjectionSplitter;
import it.unibz.inf.ontop.answering.reformulation.generation.normalization.DialectExtraNormalizer;
import it.unibz.inf.ontop.datalog.UnionFlattener;
import it.unibz.inf.ontop.dbschema.DBMetadata;
import it.unibz.inf.ontop.dbschema.RDBMetadata;
import it.unibz.inf.ontop.exception.MinorOntopInternalBugException;
import it.unibz.inf.ontop.exception.OntopReformulationException;
import it.unibz.inf.ontop.injection.IntermediateQueryFactory;
import it.unibz.inf.ontop.injection.OptimizerFactory;
import it.unibz.inf.ontop.iq.IQ;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.iq.IntermediateQuery;
import it.unibz.inf.ontop.iq.UnaryIQTree;
import it.unibz.inf.ontop.iq.exception.EmptyQueryException;
import it.unibz.inf.ontop.iq.node.*;
import it.unibz.inf.ontop.iq.optimizer.PostProcessableFunctionLifter;
import it.unibz.inf.ontop.iq.optimizer.TermTypeTermLifter;
import it.unibz.inf.ontop.iq.optimizer.PushDownBooleanExpressionOptimizer;
import it.unibz.inf.ontop.iq.optimizer.PushUpBooleanExpressionOptimizer;
import it.unibz.inf.ontop.iq.tools.ExecutorRegistry;
import it.unibz.inf.ontop.iq.tools.IQConverter;
import it.unibz.inf.ontop.model.atom.AtomFactory;
import it.unibz.inf.ontop.model.atom.DistinctVariableOnlyDataAtom;
import it.unibz.inf.ontop.utils.VariableGenerator;
import org.slf4j.LoggerFactory;

/**
 * TODO: explain
 *
 * See TranslationFactory for creating a new instance.
 *
 */
public class SQLGeneratorImpl implements NativeQueryGenerator {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SQLGeneratorImpl.class);
    private final RDBMetadata metadata;
    private final IntermediateQueryFactory iqFactory;
    private final IQConverter iqConverter;
    private final UnionFlattener unionFlattener;
    private final PushDownBooleanExpressionOptimizer pushDownExpressionOptimizer;
    private final AtomFactory atomFactory;
    private final OptimizerFactory optimizerFactory;
    private final PushUpBooleanExpressionOptimizer pullUpExpressionOptimizer;
    private final PostProcessingProjectionSplitter projectionSplitter;
    private final TermTypeTermLifter rdfTypeLifter;
    private final PostProcessableFunctionLifter functionLifter;
    private final IQTree2NativeNodeGenerator defaultIQTree2NativeNodeGenerator;
    private final DialectExtraNormalizer extraNormalizer;

    @AssistedInject
    private SQLGeneratorImpl(@Assisted DBMetadata metadata,
                             IntermediateQueryFactory iqFactory,
                             IQConverter iqConverter, UnionFlattener unionFlattener,
                             PushDownBooleanExpressionOptimizer pushDownExpressionOptimizer,
                             AtomFactory atomFactory, OptimizerFactory optimizerFactory,
                             PushUpBooleanExpressionOptimizer pullUpExpressionOptimizer,
                             PostProcessingProjectionSplitter projectionSplitter,
                             TermTypeTermLifter rdfTypeLifter, PostProcessableFunctionLifter functionLifter,
                             IQTree2NativeNodeGenerator defaultIQTree2NativeNodeGenerator,
                             DialectExtraNormalizer extraNormalizer)
    {
        this.functionLifter = functionLifter;
        this.extraNormalizer = extraNormalizer;
        if (!(metadata instanceof RDBMetadata)) {
            throw new IllegalArgumentException("Not a DBMetadata!");
        }
        this.metadata = (RDBMetadata) metadata;
        this.iqFactory = iqFactory;
        this.iqConverter = iqConverter;
        this.unionFlattener = unionFlattener;
        this.pushDownExpressionOptimizer = pushDownExpressionOptimizer;
        this.atomFactory = atomFactory;
        this.optimizerFactory = optimizerFactory;
        this.pullUpExpressionOptimizer = pullUpExpressionOptimizer;
        this.projectionSplitter = projectionSplitter;
        this.rdfTypeLifter = rdfTypeLifter;
        this.defaultIQTree2NativeNodeGenerator = defaultIQTree2NativeNodeGenerator;
    }

    @Override
    public IQ generateSourceQuery(IQ query, ExecutorRegistry executorRegistry)
            throws OntopReformulationException {

        IQ rdfTypeLiftedIQ = rdfTypeLifter.optimize(query);
        log.debug("After lifting the RDF types:\n" + rdfTypeLiftedIQ);

        IQ liftedIQ = functionLifter.optimize(rdfTypeLiftedIQ);
        log.debug("After lifting the post-processable function symbols :\n" + liftedIQ);

        PostProcessingProjectionSplitter.PostProcessingSplit split = projectionSplitter.split(liftedIQ);

        IQTree normalizedSubTree = normalizeSubTree(split.getSubTree(), split.getVariableGenerator(), executorRegistry);
        NativeNode nativeNode = generateNativeNode(normalizedSubTree);

        UnaryIQTree newTree = iqFactory.createUnaryIQTree(split.getPostProcessingConstructionNode(), nativeNode);

        return iqFactory.createIQ(query.getProjectionAtom(), newTree);
    }

    /**
     * TODO: what about the distinct?
     * TODO: move the distinct and slice lifting to the post-processing splitter
     *
     * TODO: check what is still needed
     */
    private IQTree normalizeSubTree(IQTree subTree, VariableGenerator variableGenerator,
                                    ExecutorRegistry executorRegistry) {

        IQTree sliceLiftedTree = liftSlice(subTree);
        log.debug("New query after lifting the slice: \n" + sliceLiftedTree);

        IQTree flattenSubTree = unionFlattener.optimize(sliceLiftedTree, variableGenerator);
        log.debug("New query after flattening the union: \n" + flattenSubTree);

        // Just here for converting the IQTree into an IntermediateQuery (will be ignored later on)
        DistinctVariableOnlyDataAtom temporaryProjectionAtom = atomFactory.getDistinctVariableOnlyDataAtom(
                atomFactory.getRDFAnswerPredicate(flattenSubTree.getVariables().size()),
                ImmutableList.copyOf(flattenSubTree.getVariables()));

        try {
            IQTree treeAfterPullOut = optimizerFactory.createEETransformer(variableGenerator).transform(flattenSubTree);
            log.debug("Query tree after pulling out equalities: \n" + treeAfterPullOut);


            IQ pulledOutSubQuery = iqFactory.createIQ(temporaryProjectionAtom, treeAfterPullOut);

            // Trick for pushing down expressions under unions:
            //   - there the context may be concrete enough for evaluating certain expressions
            //   - useful for dealing with SPARQL EBVs for instance
            IntermediateQuery pushedDownQuery = pushDownExpressionOptimizer.optimize(
                    iqConverter.convert(pulledOutSubQuery, executorRegistry));
            log.debug("New query after pushing down the boolean expressions (temporary): \n" + pushedDownQuery);


            // Pulling up is needed when filtering conditions appear above a data atom on the left
            // (was causing problems to the IQ2DatalogConverter)
            // TODO: check if still needed
            IntermediateQuery queryAfterPullUp = pullUpExpressionOptimizer.optimize(pushedDownQuery);
            log.debug("New query after pulling up the boolean expressions: \n" + queryAfterPullUp);

            // Dialect specific
            IQTree afterDialectNormalization = extraNormalizer.transform(iqConverter.convert(queryAfterPullUp).getTree(), variableGenerator);
            log.debug("New query after the dialect-specific extra normalization: \n" + afterDialectNormalization);
            return afterDialectNormalization;

        } catch (EmptyQueryException e) {
            // Not expected
            throw new MinorOntopInternalBugException(e.getMessage());
        }
    }

    private IQTree liftSlice(IQTree subTree) {
        if (subTree.getRootNode() instanceof ConstructionNode) {
            ConstructionNode constructionNode = (ConstructionNode) subTree.getRootNode();
            IQTree childTree = ((UnaryIQTree) subTree).getChild();
            if (childTree.getRootNode() instanceof SliceNode) {
                /*
                 * Swap the top construction node and the slice
                 */
                SliceNode sliceNode = (SliceNode) childTree.getRootNode();
                IQTree grandChildTree = ((UnaryIQTree) childTree).getChild();

                return iqFactory.createUnaryIQTree(sliceNode,
                        iqFactory.createUnaryIQTree(constructionNode, grandChildTree));
            }
        }
        return subTree;
    }

    private NativeNode generateNativeNode(IQTree normalizedSubTree) {
        return defaultIQTree2NativeNodeGenerator.generate(normalizedSubTree, metadata.getDBParameters());
    }
}
