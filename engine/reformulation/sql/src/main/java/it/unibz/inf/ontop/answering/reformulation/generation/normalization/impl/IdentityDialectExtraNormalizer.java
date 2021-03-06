package it.unibz.inf.ontop.answering.reformulation.generation.normalization.impl;

import com.google.inject.Inject;
import it.unibz.inf.ontop.answering.reformulation.generation.normalization.DialectExtraNormalizer;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.utils.VariableGenerator;

/**
 * Does nothing
 */
public class IdentityDialectExtraNormalizer implements DialectExtraNormalizer {

    @Inject
    private IdentityDialectExtraNormalizer() {
    }

    @Override
    public IQTree transform(IQTree tree, VariableGenerator variableGenerator) {
        return tree;
    }
}
