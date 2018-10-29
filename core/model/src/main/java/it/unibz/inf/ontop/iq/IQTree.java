package it.unibz.inf.ontop.iq;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.iq.exception.InvalidIntermediateQueryException;
import it.unibz.inf.ontop.iq.node.QueryNode;
import it.unibz.inf.ontop.iq.node.VariableNullability;
import it.unibz.inf.ontop.iq.transform.IQTreeVisitingTransformer;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.model.type.TermType;
import it.unibz.inf.ontop.substitution.ImmutableSubstitution;
import it.unibz.inf.ontop.utils.VariableGenerator;

import java.util.Optional;


public interface IQTree {

    QueryNode getRootNode();

    ImmutableList<IQTree> getChildren();

    /**
     * Variables projected by the tree
     */
    ImmutableSet<Variable> getVariables();

    IQTree acceptTransformer(IQTreeVisitingTransformer transformer);

    IQTree liftBinding(VariableGenerator variableGenerator);

    /**
     * Tries to lift unions when they have incompatible definitions
     * for a variable.
     *
     * Union branches with compatible definitions are kept together
     *
     * Assumes that a "regular" binding lift has already been applied
     *   --> the remaining "non-lifted" definitions are conflicting with
     *       others.
     */
    IQTree liftIncompatibleDefinitions(Variable variable);

    default boolean isLeaf() {
        return getChildren().isEmpty();
    }

    /**
     * Applies the descending substitution and performs SOME optimizations.
     *
     * Designed to be called DURING the "structural/semantic optimization" phase.
     *
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    IQTree applyDescendingSubstitution(
            ImmutableSubstitution<? extends VariableOrGroundTerm> descendingSubstitution,
            Optional<ImmutableExpression> constraint);

    /**
     * Applies the descending substitution WITHOUT applying any additional optimization.
     *
     * Designed to be called AFTER the "structural/semantic optimization" phase.
     */
    IQTree applyDescendingSubstitutionWithoutOptimizing(ImmutableSubstitution<? extends VariableOrGroundTerm> descendingSubstitution);

    /**
     * Variables present in the tree
     */
    ImmutableSet<Variable> getKnownVariables();

    /**
     * Returns true if the variable is (at least in one branch) constructed by a substitution
     * (in a construction node)
     */
    boolean isConstructed(Variable variable);

    /**
     * Returns true if corresponds to a EmptyNode
     */
    boolean isDeclaredAsEmpty();

    VariableNullability getVariableNullability();

    boolean isEquivalentTo(IQTree tree);

    /**
     * TODO: explain
     *
     * The constraint is used for pruning. It remains enforced by
     * a parent tree.
     *
     */
    IQTree propagateDownConstraint(ImmutableExpression constraint);

    void validate() throws InvalidIntermediateQueryException;

    /**
     * If subTreeToReplace is not found, has no effect (besides creating a novel copy).
     */
    IQTree replaceSubTree(IQTree subTreeToReplace, IQTree newSubTree);

    /**
     * Returns a set of substitutions that define the projected variables when they are constructed.
     *
     * Guarantee: each tuple in the result set having constructed values matches at least one of these substitutions.
     *
     * It is advised to call this method on a normalized IQ so as to reduce the number of possible substitutions
     * (normalization drops unsatisfiable substitutions).
     *
     *
     * The intended usage of this method it to determine the definition of a variable from the returned substitution.
     * Therefore this method is robust to simple variable renaming and to multiple complex substitutions.
     * E.g. if [x/x2], [x2/x3] and [x3/URI("http://myURI{}", x4] are three substitutions found in that order in a same
     * branch,
     * then x/URI("http://myURI{}", x4) will be the only output substitution for that branch.
     *
     */
    ImmutableSet<ImmutableSubstitution<NonVariableTerm>> getPossibleVariableDefinitions();

}
