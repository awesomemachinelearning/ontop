package it.unibz.inf.ontop.model.term.functionsymbol.impl;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unibz.inf.ontop.exception.MinorOntopInternalBugException;
import it.unibz.inf.ontop.iq.node.VariableNullability;
import it.unibz.inf.ontop.iq.tools.TypeConstantDictionary;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.model.term.functionsymbol.RDFTermTypeFunctionSymbol;
import it.unibz.inf.ontop.model.type.MetaRDFTermType;
import it.unibz.inf.ontop.model.type.TermTypeInference;
import it.unibz.inf.ontop.model.type.TypeFactory;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class RDFTermTypeFunctionSymbolImpl extends FunctionSymbolImpl implements RDFTermTypeFunctionSymbol {


    private final MetaRDFTermType metaType;
    private final TypeConstantDictionary dictionary;
    private final ImmutableBiMap<DBConstant, RDFTermTypeConstant> conversionMap;

    protected RDFTermTypeFunctionSymbolImpl(TypeFactory typeFactory,
                                            TypeConstantDictionary dictionary,
                                            ImmutableBiMap<DBConstant, RDFTermTypeConstant> conversionMap) {
        super("RDF_TYPE" + extractConversionMapString(conversionMap),
                ImmutableList.of(typeFactory.getDBTypeFactory().getDBBooleanType()));
        metaType = typeFactory.getMetaRDFTermType();
        this.dictionary = dictionary;
        this.conversionMap = conversionMap;
    }

    private static String extractConversionMapString(ImmutableBiMap<DBConstant, RDFTermTypeConstant> conversionMap) {
        return conversionMap.entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> e.getKey().getValue(),
                        Map.Entry::getValue))
                .toString()
                .replace(" ", "");
    }

    @Override
    public boolean isAlwaysInjectiveInTheAbsenceOfNonInjectiveFunctionalTerms() {
        return true;
    }

    @Override
    public Optional<TermTypeInference> inferType(ImmutableList<? extends ImmutableTerm> terms) {
        return Optional.of(TermTypeInference.declareTermType(metaType));
    }

    @Override
    protected ImmutableTerm buildTermAfterEvaluation(ImmutableList<ImmutableTerm> newTerms,
                                                     TermFactory termFactory, VariableNullability variableNullability) {
        ImmutableTerm term = newTerms.get(0);
        if (term instanceof DBConstant) {
            return conversionMap.get(term);
        }
        else
            return termFactory.getImmutableFunctionalTerm(this, term);
    }

    @Override
    public boolean canBePostProcessed(ImmutableList<? extends ImmutableTerm> arguments) {
        return true;
    }

    @Override
    public ImmutableBiMap<DBConstant, RDFTermTypeConstant> getConversionMap() {
        return conversionMap;
    }

    @Override
    public TypeConstantDictionary getDictionary() {
        return dictionary;
    }

    @Override
    public ImmutableTerm lift(ImmutableList<? extends ImmutableTerm> terms,
                              Function<RDFTermTypeConstant, ImmutableTerm> caseTermFct,
                              TermFactory termFactory) {
        ImmutableTerm term = terms.get(0);

        return termFactory.getDBCase(
                conversionMap.entrySet().stream()
                        .map(e -> Maps.immutableEntry(
                                // Condition
                                termFactory.getStrictEquality(term, e.getKey()),
                                // "Case" value
                                caseTermFct.apply(e.getValue()))),
                // Default case
                termFactory.getNullConstant());
    }

    @Override
    protected boolean mayReturnNullWithoutNullArguments() {
        return false;
    }

    @Override
    protected IncrementalEvaluation evaluateStrictEqWithNonNullConstant(ImmutableList<? extends ImmutableTerm> terms,
                                                                        NonNullConstant otherTerm, TermFactory termFactory,
                                                                        VariableNullability variableNullability) {
        if (!(otherTerm instanceof RDFTermTypeConstant))
            throw new MinorOntopInternalBugException("Was expecting the constant to be a RDFTermTypeConstant: " + otherTerm);
        RDFTermTypeConstant typeConstant = (RDFTermTypeConstant) otherTerm;

        return Optional.ofNullable(conversionMap.inverse().get(typeConstant))
                .map(c -> termFactory.getStrictEquality(terms.get(0), c))
                .map(IncrementalEvaluation::declareSimplifiedExpression)
                .orElseGet(IncrementalEvaluation::declareIsFalse);
    }

    @Override
    protected boolean tolerateNulls() {
        return false;
    }
}
