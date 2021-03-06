package it.unibz.inf.ontop.iq.node.normalization.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unibz.inf.ontop.iq.node.VariableNullability;
import it.unibz.inf.ontop.iq.node.impl.UnsatisfiableConditionException;
import it.unibz.inf.ontop.iq.node.normalization.ConditionSimplifier;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.model.term.functionsymbol.db.DBStrictEqFunctionSymbol;
import it.unibz.inf.ontop.substitution.ImmutableSubstitution;
import it.unibz.inf.ontop.substitution.SubstitutionFactory;
import it.unibz.inf.ontop.substitution.impl.ImmutableSubstitutionTools;
import it.unibz.inf.ontop.substitution.impl.ImmutableUnificationTools;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Singleton
public class ConditionSimplifierImpl implements ConditionSimplifier {

    private final SubstitutionFactory substitutionFactory;
    private final TermFactory termFactory;
    private final ImmutableUnificationTools unificationTools;
    private final ImmutableSubstitutionTools substitutionTools;

    @Inject
    private ConditionSimplifierImpl(SubstitutionFactory substitutionFactory,
                                    TermFactory termFactory, ImmutableUnificationTools unificationTools,
                                    ImmutableSubstitutionTools substitutionTools) {
        this.substitutionFactory = substitutionFactory;
        this.termFactory = termFactory;
        this.unificationTools = unificationTools;
        this.substitutionTools = substitutionTools;
    }


    @Override
    public ExpressionAndSubstitution simplifyCondition(ImmutableExpression expression, VariableNullability variableNullability)
            throws UnsatisfiableConditionException {
        return simplifyCondition(Optional.of(expression), ImmutableSet.of(), variableNullability);
    }

    @Override
    public ExpressionAndSubstitution simplifyCondition(Optional<ImmutableExpression> nonOptimizedExpression,
                                                       ImmutableSet<Variable> nonLiftableVariables,
                                                       VariableNullability variableNullability)
            throws UnsatisfiableConditionException {

        Optional<ImmutableExpression.Evaluation> optionalEvaluationResults =
                nonOptimizedExpression
                        .map(expression -> expression.evaluate2VL(variableNullability));

        if (optionalEvaluationResults.isPresent()) {
            ImmutableExpression.Evaluation results = optionalEvaluationResults.get();

            if (results.isEffectiveFalse())
                throw new UnsatisfiableConditionException();

            Optional<ImmutableExpression> optionalExpression = results.getExpression();
            if (optionalExpression.isPresent())
                // May throw an exception if unification is rejected
                return convertIntoExpressionAndSubstitution(optionalExpression.get(), nonLiftableVariables);
            else
                return new ExpressionAndSubstitutionImpl(Optional.empty(), substitutionFactory.getSubstitution());
        }
        else
            return new ExpressionAndSubstitutionImpl(Optional.empty(), substitutionFactory.getSubstitution());
    }

    /**
     * TODO: explain
     *
     * Functional terms remain in the expression (never going into the substitution)
     *
     */
    private ExpressionAndSubstitution convertIntoExpressionAndSubstitution(ImmutableExpression expression,
                                                                                                      ImmutableSet<Variable> nonLiftableVariables)
            throws UnsatisfiableConditionException {

        ImmutableSet<ImmutableExpression> expressions = expression.flattenAND()
                .collect(ImmutableCollectors.toSet());
        ImmutableSet<ImmutableExpression> functionFreeEqualities = expressions.stream()
                .filter(e -> e.getFunctionSymbol() instanceof DBStrictEqFunctionSymbol)
                // TODO: consider the fact that equalities might be n-ary
                .filter(e -> {
                    ImmutableList<? extends ImmutableTerm> arguments = e.getTerms();
                    return arguments.stream().allMatch(t -> t instanceof NonFunctionalTerm);
                })
                .collect(ImmutableCollectors.toSet());

        ImmutableSubstitution<NonFunctionalTerm> normalizedUnifier = unify(functionFreeEqualities.stream()
                        .map(ImmutableFunctionalTerm::getTerms)
                        .map(args -> Maps.immutableEntry(
                                (NonFunctionalTerm) args.get(0),
                                (NonFunctionalTerm)args.get(1))),
                nonLiftableVariables);

        Optional<ImmutableExpression> newExpression = termFactory.getConjunction(
                Stream.concat(
                        // Expressions that are not function-free equalities
                        expressions.stream()
                                .filter(e -> !functionFreeEqualities.contains(e))
                                .map(normalizedUnifier::applyToBooleanExpression),

                        // Equalities that must remain
                        normalizedUnifier.getImmutableMap().entrySet().stream()
                                .filter(e -> nonLiftableVariables.contains(e.getKey()))
                                .map(e -> termFactory.getStrictEquality(e.getKey(), e.getValue()))
                ));

        ImmutableSubstitution<NonFunctionalTerm> ascendingSubstitution = substitutionFactory.getSubstitution(
                normalizedUnifier.getImmutableMap().entrySet().stream()
                        .filter(e -> !nonLiftableVariables.contains(e.getKey()))
                        .collect(ImmutableCollectors.toMap()));

        return new ExpressionAndSubstitutionImpl(newExpression, ascendingSubstitution);
    }

    private ImmutableSubstitution<NonFunctionalTerm> unify(
            Stream<Map.Entry<NonFunctionalTerm, NonFunctionalTerm>> equalityStream,
            ImmutableSet<Variable> nonLiftableVariables) throws UnsatisfiableConditionException {
        ImmutableList<Map.Entry<NonFunctionalTerm, NonFunctionalTerm>> equalities = equalityStream.collect(ImmutableCollectors.toList());

        ImmutableList<NonFunctionalTerm> args1 = equalities.stream()
                .map(Map.Entry::getKey)
                .collect(ImmutableCollectors.toList());

        ImmutableList<NonFunctionalTerm> args2 = equalities.stream()
                .map(Map.Entry::getValue)
                .collect(ImmutableCollectors.toList());

        return unificationTools.computeMGU(args1, args2)
                // TODO: merge priorityRenaming with the orientate() method
                .map(u -> substitutionTools.prioritizeRenaming(u, nonLiftableVariables))
                .orElseThrow(UnsatisfiableConditionException::new);
    }

    @Override
    public Optional<ImmutableExpression> computeDownConstraint(Optional<ImmutableExpression> optionalConstraint,
                                                               ExpressionAndSubstitution conditionSimplificationResults,
                                                               VariableNullability childVariableNullability)
            throws UnsatisfiableConditionException {
        if (optionalConstraint.isPresent()) {
            ImmutableExpression substitutedConstraint = conditionSimplificationResults.getSubstitution()
                    .applyToBooleanExpression(optionalConstraint.get());

            ImmutableExpression combinedExpression = conditionSimplificationResults.getOptionalExpression()
                    .flatMap(e -> termFactory.getConjunction(
                            Stream.concat(
                                    e.flattenAND(),
                                    substitutedConstraint.flattenAND())))
                    .orElse(substitutedConstraint);

            ImmutableExpression.Evaluation evaluationResults = combinedExpression.evaluate2VL(childVariableNullability);

            if (evaluationResults.isEffectiveFalse())
                throw new UnsatisfiableConditionException();

            return evaluationResults.getExpression();
        }
        else
            return conditionSimplificationResults.getOptionalExpression();
    }


}
