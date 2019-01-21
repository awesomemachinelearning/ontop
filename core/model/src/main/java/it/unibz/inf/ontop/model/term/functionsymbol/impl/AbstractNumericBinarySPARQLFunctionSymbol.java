package it.unibz.inf.ontop.model.term.functionsymbol.impl;

import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.iq.node.VariableNullability;
import it.unibz.inf.ontop.model.term.ImmutableFunctionalTerm;
import it.unibz.inf.ontop.model.term.ImmutableTerm;
import it.unibz.inf.ontop.model.term.TermFactory;
import it.unibz.inf.ontop.model.type.RDFDatatype;
import it.unibz.inf.ontop.model.type.TermTypeInference;

import java.util.Optional;

public abstract class AbstractNumericBinarySPARQLFunctionSymbol extends ReduciblePositiveAritySPARQLFunctionSymbolImpl {

    protected AbstractNumericBinarySPARQLFunctionSymbol(String functionSymbolName, String officialName,
                                                        RDFDatatype abstractNumericType) {
        super(functionSymbolName, officialName, ImmutableList.of(abstractNumericType, abstractNumericType));
    }

    @Override
    protected ImmutableTerm computeTypeTerm(ImmutableList<? extends ImmutableTerm> subLexicalTerms,
                                            ImmutableList<ImmutableTerm> typeTerms, TermFactory termFactory,
                                            VariableNullability variableNullability) {
        return termFactory.getCommonPropagatedOrSubstitutedNumericType(typeTerms.get(0), typeTerms.get(1));
    }

    @Override
    protected ImmutableTerm computeLexicalTerm(ImmutableList<ImmutableTerm> subLexicalTerms,
                                               ImmutableList<ImmutableTerm> typeTerms, TermFactory termFactory,
                                               ImmutableTerm returnedRDFTypeTerm) {

        ImmutableTerm numericTerm = computeNumericTerm(
                termFactory.getConversionFromRDFLexical2NaturalDB(subLexicalTerms.get(0), returnedRDFTypeTerm),
                termFactory.getConversionFromRDFLexical2NaturalDB(subLexicalTerms.get(1), returnedRDFTypeTerm),
                termFactory);

        return termFactory.getReconversion2RDFLexical(numericTerm, returnedRDFTypeTerm);
    }

    protected abstract ImmutableTerm computeNumericTerm(ImmutableFunctionalTerm numericTerm1,
                                                        ImmutableFunctionalTerm numericTerm2,
                                                        TermFactory termFactory);

    @Override
    protected boolean isAlwaysInjective() {
        return false;
    }

    /**
     * Too complex logic so not infer at this level (but after simplification into DB functional terms)
     */
    @Override
    public Optional<TermTypeInference> inferType(ImmutableList<? extends ImmutableTerm> terms) {
        return Optional.empty();
    }

    @Override
    public boolean canBePostProcessed(ImmutableList<? extends ImmutableTerm> arguments) {
        return false;
    }
}
