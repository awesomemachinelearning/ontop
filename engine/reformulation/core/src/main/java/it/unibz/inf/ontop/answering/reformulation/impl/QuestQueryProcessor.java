package it.unibz.inf.ontop.answering.reformulation.impl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import it.unibz.inf.ontop.answering.reformulation.QueryCache;
import it.unibz.inf.ontop.answering.reformulation.QueryReformulator;
import it.unibz.inf.ontop.answering.reformulation.generation.NativeQueryGenerator;
import it.unibz.inf.ontop.answering.reformulation.input.InputQuery;
import it.unibz.inf.ontop.answering.reformulation.input.InputQueryFactory;
import it.unibz.inf.ontop.answering.reformulation.input.translation.InputQueryTranslator;
import it.unibz.inf.ontop.answering.reformulation.rewriting.QueryRewriter;
import it.unibz.inf.ontop.answering.reformulation.unfolding.QueryUnfolder;
import it.unibz.inf.ontop.dbschema.DBMetadata;
import it.unibz.inf.ontop.exception.OntopReformulationException;
import it.unibz.inf.ontop.injection.IntermediateQueryFactory;
import it.unibz.inf.ontop.injection.TranslationFactory;
import it.unibz.inf.ontop.iq.IQ;
import it.unibz.inf.ontop.iq.IntermediateQuery;
import it.unibz.inf.ontop.iq.exception.EmptyQueryException;
import it.unibz.inf.ontop.iq.optimizer.*;
import it.unibz.inf.ontop.iq.tools.ExecutorRegistry;
import it.unibz.inf.ontop.iq.tools.IQConverter;
import it.unibz.inf.ontop.model.atom.AtomFactory;
import it.unibz.inf.ontop.model.atom.DistinctVariableOnlyDataAtom;
import it.unibz.inf.ontop.model.term.Variable;
import it.unibz.inf.ontop.spec.OBDASpecification;
import it.unibz.inf.ontop.spec.mapping.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: rename it QueryTranslatorImpl ?
 *
 * See ReformulationFactory for creating a new instance.
 *
 */
public class QuestQueryProcessor implements QueryReformulator {

	private final QueryRewriter rewriter;
	private final NativeQueryGenerator datasourceQueryGenerator;
	private final QueryCache queryCache;

	private final QueryUnfolder queryUnfolder;
	private final UnionAndBindingLiftOptimizer bindingLiftOptimizer;

	private static final Logger log = LoggerFactory.getLogger(QuestQueryProcessor.class);
	private final ExecutorRegistry executorRegistry;
	private final DBMetadata dbMetadata;
	private final JoinLikeOptimizer joinLikeOptimizer;
	private final InputQueryTranslator inputQueryTranslator;
	private final InputQueryFactory inputQueryFactory;
	private final FlattenUnionOptimizer flattenUnionOptimizer;
	private final PushUpBooleanExpressionOptimizer pullUpExpressionOptimizer;
	private final IQConverter iqConverter;
	private final AtomFactory atomFactory;
	private final IntermediateQueryFactory iqFactory;
	private final OrderBySimplifier orderBySimplifier;
	private final AggregationSimplifier aggregationSimplifier;

	@AssistedInject
	private QuestQueryProcessor(@Assisted OBDASpecification obdaSpecification,
								@Assisted ExecutorRegistry executorRegistry,
								QueryCache queryCache,
								UnionAndBindingLiftOptimizer bindingLiftOptimizer,
								TranslationFactory translationFactory,
								QueryRewriter queryRewriter,
								JoinLikeOptimizer joinLikeOptimizer,
								InputQueryFactory inputQueryFactory,
								FlattenUnionOptimizer flattenUnionOptimizer,
								PushUpBooleanExpressionOptimizer pullUpExpressionOptimizer,
								InputQueryTranslator inputQueryTranslator,
								IQConverter iqConverter,
								AtomFactory atomFactory, IntermediateQueryFactory iqFactory,
								OrderBySimplifier orderBySimplifier, AggregationSimplifier aggregationSimplifier) {
		this.bindingLiftOptimizer = bindingLiftOptimizer;
		this.joinLikeOptimizer = joinLikeOptimizer;
		this.inputQueryFactory = inputQueryFactory;
		this.flattenUnionOptimizer = flattenUnionOptimizer;
		this.pullUpExpressionOptimizer = pullUpExpressionOptimizer;
		this.iqConverter = iqConverter;
		this.rewriter = queryRewriter;
		this.atomFactory = atomFactory;
		this.iqFactory = iqFactory;
		this.orderBySimplifier = orderBySimplifier;
		this.aggregationSimplifier = aggregationSimplifier;

		this.rewriter.setTBox(obdaSpecification.getSaturatedTBox());

		Mapping saturatedMapping = obdaSpecification.getSaturatedMapping();

		if(log.isDebugEnabled()){
			log.debug("Mapping: \n{}", Joiner.on("\n").join(
					saturatedMapping.getRDFAtomPredicates().stream()
						.flatMap(p -> saturatedMapping.getQueries(p).stream())
						.iterator()));
		}

		this.queryUnfolder = translationFactory.create(saturatedMapping);

		this.dbMetadata = obdaSpecification.getDBMetadata();
		this.datasourceQueryGenerator = translationFactory.create(dbMetadata);
		this.inputQueryTranslator = inputQueryTranslator;
		this.queryCache = queryCache;
		this.executorRegistry = executorRegistry;

		log.info("Ontop has completed the setup and it is ready for query answering!");
	}

	@Override
	public IQ reformulateIntoNativeQuery(InputQuery inputQuery)
			throws OntopReformulationException {

		IQ cachedQuery = queryCache.get(inputQuery);
		if (cachedQuery != null)
			return cachedQuery;

		try {


			log.debug("SPARQL query:\n{}", inputQuery.getInputString());
			IQ convertedIQ = inputQuery.translate(inputQueryTranslator);
			log.debug("Parsed query converted into IQ (after normalization):\n{}", convertedIQ);
			//InternalSparqlQuery translation = inputQuery.translate(inputQueryTranslator);

            try {
             //   IQ convertedIQ = preProcess(translation);

                log.debug("Start the rewriting process...");
                IQ rewrittenIQ = rewriter.rewrite(convertedIQ);

                log.debug("Rewritten IQ:\n{}",rewrittenIQ);

                log.debug("Start the unfolding...");

                IQ unfoldedIQ = queryUnfolder.optimize(rewrittenIQ);
                if (unfoldedIQ.getTree().isDeclaredAsEmpty())
                    throw new EmptyQueryException();
                log.debug("Unfolded query: \n" + unfoldedIQ.toString());

                //lift bindings and union when it is possible
				IQ liftedQuery = bindingLiftOptimizer.optimize(unfoldedIQ);
                log.debug("New lifted query: \n" + liftedQuery.toString());

				// Non-final
				IntermediateQuery intermediateQuery = pullUpExpressionOptimizer.optimize(iqConverter.convert(liftedQuery, executorRegistry));
                log.debug("After pushing up boolean expressions: \n" + intermediateQuery.toString());

                intermediateQuery = new ProjectionShrinkingOptimizer().optimize(intermediateQuery);

                log.debug("After projection shrinking: \n" + intermediateQuery.toString());


                intermediateQuery = joinLikeOptimizer.optimize(intermediateQuery);
                log.debug("New query after fixed point join optimization: \n" + intermediateQuery.toString());

                intermediateQuery = flattenUnionOptimizer.optimize(intermediateQuery);
                log.debug("New query after flattening Unions: \n" + intermediateQuery.toString());

                IQ queryAfterAggregationSimplification = aggregationSimplifier.optimize(iqConverter.convert(intermediateQuery));
 				log.debug("New query after simplifying the aggregation node: \n" + queryAfterAggregationSimplification);
				IQ optimizedQuery = orderBySimplifier.optimize(queryAfterAggregationSimplification);
				log.debug("New query after simplifying the order by node: \n" + optimizedQuery);

				IQ executableQuery = generateExecutableQuery(optimizedQuery);
				queryCache.put(inputQuery, executableQuery);
				return executableQuery;

			}
			catch (EmptyQueryException e) {
				ImmutableList<Variable> signature = convertedIQ.getProjectionAtom().getArguments();

				DistinctVariableOnlyDataAtom projectionAtom = atomFactory.getDistinctVariableOnlyDataAtom(
						atomFactory.getRDFAnswerPredicate(signature.size()),
						signature);

				return iqFactory.createIQ(projectionAtom,
						iqFactory.createEmptyNode(projectionAtom.getVariables()));
            }
            catch (OntopReformulationException e) {
                throw e;
            }
        }
		/*
		 * Bug: should normally not be reached
		 * TODO: remove it
		 */
		catch (Exception e) {
			log.warn("Unexpected exception: " + e.getMessage(), e);
			throw new OntopReformulationException(e);
			//throw new OntopReformulationException("Error rewriting and unfolding into SQL\n" + e.getMessage());
		}
	}

	private IQ generateExecutableQuery(IQ iq)
			throws OntopReformulationException {
		log.debug("Producing the native query string...");

		IQ executableQuery = datasourceQueryGenerator.generateSourceQuery(iq, executorRegistry);

		log.debug("Resulting native query: \n{}", executableQuery);

		return executableQuery;
	}


	/**
	 * Returns the final rewriting of the given query
	 */
	@Override
	public String getRewritingRendering(InputQuery query) throws OntopReformulationException {
		log.debug("SPARQL query:\n{}", query.getInputString());
		IQ convertedIQ = query.translate(inputQueryTranslator);
		log.debug("Parsed query converted into IQ:\n{}", convertedIQ);
		try {
          //  IQ convertedIQ = preProcess(translation);
			IQ rewrittenIQ = rewriter.rewrite(convertedIQ);
			return rewrittenIQ.toString();
		}
		catch (EmptyQueryException e) {
			e.printStackTrace();
		}
		return "EMPTY REWRITING";
	}

	@Override
	public InputQueryFactory getInputQueryFactory() {
		return inputQueryFactory;
	}
}
