package it.unibz.inf.ontop.model.term.functionsymbol.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import it.unibz.inf.ontop.exception.MinorOntopInternalBugException;
import it.unibz.inf.ontop.iq.node.VariableNullability;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.model.term.ImmutableFunctionalTerm.FunctionalTermDecomposition;
import it.unibz.inf.ontop.model.term.functionsymbol.BooleanFunctionSymbol;
import it.unibz.inf.ontop.model.term.functionsymbol.FunctionSymbol;
import it.unibz.inf.ontop.model.term.functionsymbol.db.DBIfElseNullFunctionSymbol;
import it.unibz.inf.ontop.model.term.functionsymbol.db.DBIfThenFunctionSymbol;
import it.unibz.inf.ontop.model.term.functionsymbol.db.NonDeterministicDBFunctionSymbol;
import it.unibz.inf.ontop.model.term.impl.FunctionalTermNullabilityImpl;
import it.unibz.inf.ontop.model.term.impl.PredicateImpl;
import it.unibz.inf.ontop.model.type.TermType;
import it.unibz.inf.ontop.model.type.TermTypeInference;
import it.unibz.inf.ontop.utils.ImmutableCollectors;
import it.unibz.inf.ontop.utils.VariableGenerator;
import it.unibz.inf.ontop.utils.impl.VariableGeneratorImpl;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class FunctionSymbolImpl extends PredicateImpl implements FunctionSymbol {

    private final ImmutableList<TermType> expectedBaseTypes;

    protected FunctionSymbolImpl(@Nonnull String name,
                                 @Nonnull ImmutableList<TermType> expectedBaseTypes) {
        super(name, expectedBaseTypes.size());
        this.expectedBaseTypes = expectedBaseTypes;
    }

    @Override
    public FunctionalTermNullability evaluateNullability(ImmutableList<? extends NonFunctionalTerm> arguments,
                                                         VariableNullability childNullability, TermFactory termFactory) {
        IncrementalEvaluation evaluation = evaluateIsNotNull(transformIntoRegularArguments(arguments, termFactory), termFactory, childNullability);
        switch (evaluation.getStatus()) {
            case SIMPLIFIED_EXPRESSION:
                return evaluation.getNewExpression()
                        .filter(e -> e.getFunctionSymbol().equals(termFactory.getDBFunctionSymbolFactory().getDBIsNotNull()))
                        .map(e -> e.getTerm(0))
                        .filter(t -> t instanceof Variable)
                        .map(t -> (Variable)t)
                        // Bound to that variable
                        .map(FunctionalTermNullabilityImpl::new)
                        // Depends on multiple variables -> is not bound to a variable
                        .orElseGet(() -> new FunctionalTermNullabilityImpl(true));
            case IS_NULL:
                throw new MinorOntopInternalBugException("An IS_NOT_NULL cannot evaluate to NULL");
            case IS_TRUE:
                return new FunctionalTermNullabilityImpl(false);
            case IS_FALSE:
            case SAME_EXPRESSION:
            default:
                return new FunctionalTermNullabilityImpl(true);
        }
    }

    /**
     * By default, reuses the same arguments
     *
     * Needed to be overridden by function symbols that require EXPRESSIONS for some of their arguments
     */
    protected ImmutableList<? extends ImmutableTerm> transformIntoRegularArguments(
            ImmutableList<? extends NonFunctionalTerm> arguments, TermFactory termFactory) {
        return arguments;
    }

    @Override
    public ImmutableTerm simplify(ImmutableList<? extends ImmutableTerm> terms,
                                  TermFactory termFactory, VariableNullability variableNullability) {

        ImmutableList<ImmutableTerm> newTerms = terms.stream()
                .map(t -> (t instanceof ImmutableFunctionalTerm)
                        ? t.simplify(variableNullability)
                        : t)
                .collect(ImmutableCollectors.toList());

        if ((!tolerateNulls()) && newTerms.stream().anyMatch(t -> (t instanceof Constant) && t.isNull()))
            return termFactory.getNullConstant();

        return simplifyIfElseNull(newTerms, termFactory, variableNullability)
                .orElseGet(() -> buildTermAfterEvaluation(newTerms, termFactory, variableNullability));
    }

    /**
     * If one arguments is a IF_ELSE_NULL(...) functional term, tries to lift the IF_ELSE_NULL above.
     *
     * Lifting is only possible for function symbols that do not tolerate nulls.
     *
     */
    private Optional<ImmutableTerm> simplifyIfElseNull(ImmutableList<ImmutableTerm> terms, TermFactory termFactory,
                                                       VariableNullability variableNullability) {
        if ((!enableIfElseNullLifting())
                || tolerateNulls()
                // Avoids infinite loops
                || (this instanceof DBIfElseNullFunctionSymbol))
            return Optional.empty();

        return IntStream.range(0, terms.size())
                .filter(i -> {
                    ImmutableTerm term = terms.get(i);
                    return (term instanceof ImmutableFunctionalTerm)
                            && (((ImmutableFunctionalTerm) term).getFunctionSymbol() instanceof DBIfElseNullFunctionSymbol);
                })
                .boxed()
                .findAny()
                .map(i -> liftIfElseNull(terms, i, termFactory, variableNullability));
    }

    /**
     * Lifts the IF_ELSE_NULL above the current functional term
     */
    private ImmutableTerm liftIfElseNull(ImmutableList<ImmutableTerm> terms, int index, TermFactory termFactory,
                                         VariableNullability variableNullability) {
        ImmutableFunctionalTerm ifElseNullTerm = (ImmutableFunctionalTerm) terms.get(index);
        ImmutableExpression condition = (ImmutableExpression) ifElseNullTerm.getTerm(0);
        ImmutableTerm conditionalTerm = ifElseNullTerm.getTerm(1);

        ImmutableList<ImmutableTerm> newTerms = IntStream.range(0, terms.size())
                .boxed()
                .map(i -> i == index ? conditionalTerm : terms.get(i))
                .collect(ImmutableCollectors.toList());

        ImmutableFunctionalTerm newFunctionalTerm = (this instanceof BooleanFunctionSymbol)
                ? termFactory.getBooleanIfElseNull(condition,
                termFactory.getImmutableExpression((BooleanFunctionSymbol) this, newTerms))
                : termFactory.getIfElseNull(condition,
                termFactory.getImmutableFunctionalTerm(this, newTerms));

        return newFunctionalTerm.simplify(variableNullability);
    }

    /**
     * Default implementation, to be overridden to convert more cases
     *
     * Incoming terms are not simplified as they are presumed to be already simplified
     *  (so please simplify them before)
     *
     */
    @Override
    public IncrementalEvaluation evaluateStrictEq(ImmutableList<? extends ImmutableTerm> terms, ImmutableTerm otherTerm,
                                                  TermFactory termFactory, VariableNullability variableNullability) {
        boolean differentTypeDetected = inferType(terms)
                .flatMap(TermTypeInference::getTermType)
                .map(t1 -> otherTerm.inferType()
                        .flatMap(TermTypeInference::getTermType)
                        .map(t2 -> !t1.equals(t2))
                        .orElse(false))
                .orElse(false);

        if (differentTypeDetected)
            return IncrementalEvaluation.declareIsFalse();

        if ((otherTerm instanceof ImmutableFunctionalTerm))
            return evaluateStrictEqWithFunctionalTerm(terms, (ImmutableFunctionalTerm) otherTerm, termFactory,
                    variableNullability);
        else if ((otherTerm instanceof Constant) && otherTerm.isNull())
            return IncrementalEvaluation.declareIsNull();
        else if (otherTerm instanceof NonNullConstant) {
            return evaluateStrictEqWithNonNullConstant(terms, (NonNullConstant) otherTerm, termFactory, variableNullability);
        }
        return IncrementalEvaluation.declareSameExpression();
    }

    /**
     * Default implementation, can be overridden
     */
    @Override
    public IncrementalEvaluation evaluateIsNotNull(ImmutableList<? extends ImmutableTerm> terms, TermFactory termFactory,
                                                   VariableNullability variableNullability) {
        if ((!mayReturnNullWithoutNullArguments()) && (!tolerateNulls())) {
            ImmutableSet<Variable> nullableVariables = variableNullability.getNullableVariables();
            Optional<ImmutableExpression> optionalExpression = termFactory.getConjunction(terms.stream()
                    .filter(t -> (t.isNullable(nullableVariables)))
                    .map(termFactory::getDBIsNotNull));

            return optionalExpression
                    .map(e -> e.evaluate(variableNullability, true))
                    .orElseGet(IncrementalEvaluation::declareIsTrue);
        }
        // By default, does not optimize (to be overridden for optimizing)
        return IncrementalEvaluation.declareSameExpression();
    }

    @Override
    public boolean isDeterministic() {
        return !(this instanceof NonDeterministicDBFunctionSymbol);
    }

    /**
     * By default, to be overridden by function symbols that supports tolerate NULL values
     */
    @Override
    public boolean isNullable(ImmutableSet<Integer> nullableIndexes) {
        return mayReturnNullWithoutNullArguments() || (!nullableIndexes.isEmpty());
    }

    /**
     * By default, assume it is not an aggregation function symbol
     *
     * To be overridden when needed
     */
    @Override
    public boolean isAggregation() {
        return false;
    }

    /**
     * Conservative by default
     *
     * Can be overridden
     */
    @Override
    public Stream<Variable> proposeProvenanceVariables(ImmutableList<? extends ImmutableTerm> terms) {
        if (!mayReturnNullWithoutNullArguments() && (!tolerateNulls()))
            return terms.stream()
                .filter(t -> t instanceof NonConstantTerm)
                .flatMap(t -> (t instanceof Variable)
                        ? Stream.of((Variable) t)
                        : ((ImmutableFunctionalTerm)t).proposeProvenanceVariables());
        // By default
        return Stream.empty();
    }

    /**
     * Default implementation, can be overridden
     *
     */
    protected IncrementalEvaluation evaluateStrictEqWithFunctionalTerm(ImmutableList<? extends ImmutableTerm> terms,
                                                                       ImmutableFunctionalTerm otherTerm,
                                                                       TermFactory termFactory,
                                                                       VariableNullability variableNullability) {
        /*
         * In case of injectivity
         */
        if (otherTerm.getFunctionSymbol().equals(this)
                && isInjective(terms, variableNullability, termFactory)) {
            if (getArity() == 0)
                return IncrementalEvaluation.declareIsTrue();

            if (!canBeSafelyDecomposedIntoConjunction(terms, variableNullability, otherTerm.getTerms()))
                /*
                 * TODO: support this special case? Could potentially be wrapped into an IF-ELSE-NULL
                 */
                return IncrementalEvaluation.declareSameExpression();

            ImmutableExpression newExpression = termFactory.getConjunction(
                    IntStream.range(0, getArity())
                            .boxed()
                            .map(i -> termFactory.getStrictEquality(terms.get(i), otherTerm.getTerm(i)))
                            .collect(ImmutableCollectors.toList()));

            return newExpression.evaluate(variableNullability, true);
        }
        else
            return IncrementalEvaluation.declareSameExpression();
    }

    /**
     * ONLY for injective function symbols
     *
     * Makes sure that the conjunction would never evaluate as FALSE instead of NULL
     * (first produced equality evaluated as false, while the second evaluates as NULL)
     *
     */
    protected boolean canBeSafelyDecomposedIntoConjunction(ImmutableList<? extends ImmutableTerm> terms,
                                                         VariableNullability variableNullability,
                                                         ImmutableList<? extends ImmutableTerm> otherTerms) {
        if (mayReturnNullWithoutNullArguments())
            return false;
        if (getArity() == 1)
            return true;

        return !(variableNullability.canPossiblyBeNullSeparately(terms)
                || variableNullability.canPossiblyBeNullSeparately(otherTerms));
    }

    /**
     * Default implementation, does nothing, can be overridden
     */
    protected IncrementalEvaluation evaluateStrictEqWithNonNullConstant(ImmutableList<? extends ImmutableTerm> terms,
                                                                        NonNullConstant otherTerm, TermFactory termFactory,
                                                                        VariableNullability variableNullability) {
        return IncrementalEvaluation.declareSameExpression();
    }

    /**
     * Returns true if is not guaranteed to return NULL when one argument is NULL.
     *
     * Can be the case for some function symbols that are rejecting certain cases
     * and therefore refuse to simplify themselves, e.g. RDF(NULL,IRI) is invalid
     * and therefore cannot be simplified.
     *
     */
    protected abstract boolean tolerateNulls();

    /**
     * Returns false when a functional term with this symbol:
     *   1. never produce NULLs
     *   2. May produce NULLs but it is always due to a NULL argument
     */
    protected abstract boolean mayReturnNullWithoutNullArguments();

    /**
     * Returns false if IfElseNullLifting must be disabled althrough it may have been technically possible.
     *
     * False by defaults
     */
    protected boolean enableIfElseNullLifting() {
        return false;
    }

    /**
     * To be overridden when is sometimes but not always injective in the absence of non-injective functional terms
     */
    @Override
    public Optional<FunctionalTermDecomposition> analyzeInjectivity(ImmutableList<? extends ImmutableTerm> arguments,
                                                                    ImmutableSet<Variable> nonFreeVariables,
                                                                    VariableNullability variableNullability,
                                                                    VariableGenerator variableGenerator,
                                                                    TermFactory termFactory) {
        if (!isDeterministic())
            return Optional.empty();

        if (arguments.stream()
                .allMatch(t -> ((t instanceof GroundTerm) && ((GroundTerm) t).isDeterministic())
                        || nonFreeVariables.contains(t)))
            return Optional.of(termFactory.getFunctionalTermDecomposition(
                    termFactory.getImmutableFunctionalTerm(this, arguments)));

        if (!isAlwaysInjectiveInTheAbsenceOfNonInjectiveFunctionalTerms())
            return Optional.empty();

        return Optional.of(decomposeInjectiveTopFunctionalTerm(arguments, nonFreeVariables, variableNullability,
                variableGenerator, termFactory));
    }

    /**
     * Only when injectivity of the top function symbol is proved!
     */
    protected FunctionalTermDecomposition decomposeInjectiveTopFunctionalTerm(ImmutableList<? extends ImmutableTerm> arguments,
                                                                              ImmutableSet<Variable> nonFreeVariables,
                                                                              VariableNullability variableNullability,
                                                                              VariableGenerator variableGenerator,
                                                                              TermFactory termFactory) {
        ImmutableMap<Integer, Optional<FunctionalTermDecomposition>> subTermDecompositions = IntStream.range(0, getArity())
                .filter(i -> arguments.get(i) instanceof ImmutableFunctionalTerm)
                .boxed()
                .collect(ImmutableCollectors.toMap(
                        i -> i,
                        i -> ((ImmutableFunctionalTerm) arguments.get(i))
                                // Recursive
                                .analyzeInjectivity(nonFreeVariables, variableNullability, variableGenerator)));

        ImmutableList<ImmutableTerm> newArguments = IntStream.range(0, getArity())
                .boxed()
                .map(i -> Optional.ofNullable(subTermDecompositions.get(i))
                        .map(optionalDecomposition -> optionalDecomposition
                                // Injective functional sub-term
                                .map(FunctionalTermDecomposition::getLiftableTerm)
                                // Otherwise a fresh variable
                                .orElseGet(variableGenerator::generateNewVariable))
                        // Previous argument when non-functional
                        .orElseGet(() -> arguments.get(i)))
                .collect(ImmutableCollectors.toList());

        ImmutableMap<Variable, ImmutableFunctionalTerm> subTermSubstitutionMap = subTermDecompositions.entrySet().stream()
                .flatMap(e -> e.getValue()
                        // Decomposition case
                        .map(d -> d.getSubTermSubstitutionMap()
                                .map(s -> s.entrySet().stream())
                                .orElseGet(Stream::empty))
                        // Not decomposed: new entry (new variable -> functional term)
                        .orElseGet(() -> Stream.of(Maps.immutableEntry(
                                (Variable) newArguments.get(e.getKey()),
                                (ImmutableFunctionalTerm) arguments.get(e.getKey())))))
                .collect(ImmutableCollectors.toMap());

        ImmutableFunctionalTerm newFunctionalTerm = termFactory.getImmutableFunctionalTerm(this, newArguments);

        return subTermSubstitutionMap.isEmpty()
                ? termFactory.getFunctionalTermDecomposition(newFunctionalTerm)
                : termFactory.getFunctionalTermDecomposition(newFunctionalTerm, subTermSubstitutionMap);
    }

    protected final boolean isInjective(ImmutableList<? extends ImmutableTerm> arguments,
                                        VariableNullability variableNullability, TermFactory termFactory) {
        // Only for test purposes
        VariableGenerator testVariableGenerator = new VariableGeneratorImpl(
                arguments.stream()
                        .flatMap(ImmutableTerm::getVariableStream)
                        .collect(ImmutableCollectors.toSet()), termFactory);

        return analyzeInjectivity(arguments, ImmutableSet.of(), variableNullability, testVariableGenerator, termFactory)
                .filter(d -> !d.getSubTermSubstitutionMap().isPresent())
                .isPresent();
    }

    /**
     * By default, just build a new functional term.
     *
     * NB: If the function symbol does not tolerate NULL values, no need to handle them here.
     *
     */
    protected ImmutableTerm buildTermAfterEvaluation(ImmutableList<ImmutableTerm> newTerms,
                                                     TermFactory termFactory, VariableNullability variableNullability) {
        return termFactory.getImmutableFunctionalTerm(this, newTerms);
    }

    protected ImmutableList<TermType> getExpectedBaseTypes() {
        return expectedBaseTypes;
    }

    @Override
    public TermType getExpectedBaseType(int index) {
        return expectedBaseTypes.get(index);
    }
}
