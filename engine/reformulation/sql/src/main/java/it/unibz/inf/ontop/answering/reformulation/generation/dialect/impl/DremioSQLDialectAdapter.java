package it.unibz.inf.ontop.answering.reformulation.generation.dialect.impl;

public class DremioSQLDialectAdapter extends SQL99DialectAdapter {
    @Override
    public String getNullConstant() {
        return "CAST(NULL AS CHAR)";
    }
}
