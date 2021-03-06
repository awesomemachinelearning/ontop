package it.unibz.inf.ontop.model.term.functionsymbol.db.impl;

import it.unibz.inf.ontop.model.term.functionsymbol.db.BnodeStringTemplateFunctionSymbol;
import it.unibz.inf.ontop.model.type.TypeFactory;
import it.unibz.inf.ontop.utils.URITemplates;

public class BnodeStringTemplateFunctionSymbolImpl extends ObjectStringTemplateFunctionSymbolImpl
        implements BnodeStringTemplateFunctionSymbol {

    private BnodeStringTemplateFunctionSymbolImpl(String template, int arity, TypeFactory typeFactory) {
        super(template, arity, typeFactory);
    }

    public static BnodeStringTemplateFunctionSymbol createFunctionSymbol(String template,
                                                                         TypeFactory typeFactory) {
        int arity = URITemplates.getArity(template);
        return new BnodeStringTemplateFunctionSymbolImpl(template, arity, typeFactory);
    }
}
