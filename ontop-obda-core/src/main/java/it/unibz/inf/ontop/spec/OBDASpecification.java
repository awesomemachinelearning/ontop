package it.unibz.inf.ontop.spec;


import it.unibz.inf.ontop.mapping.Mapping;
import it.unibz.inf.ontop.model.DBMetadata;
import it.unibz.inf.ontop.ontology.ImmutableOntologyVocabulary;
import it.unibz.inf.ontop.owlrefplatform.core.dagjgrapht.TBoxReasoner;

/**
 * TODO: find a better name
 */
public interface OBDASpecification {

    Mapping getMapping();

    DBMetadata getDBMetadata();

    TBoxReasoner getSaturatedTBox();

    ImmutableOntologyVocabulary getVocabulary();
}
