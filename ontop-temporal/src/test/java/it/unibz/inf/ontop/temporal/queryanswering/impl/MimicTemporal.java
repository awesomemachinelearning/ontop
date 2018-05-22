package it.unibz.inf.ontop.temporal.queryanswering.impl;

import it.unibz.inf.ontop.injection.OntopTemporalSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.owlapi.OntopOWLFactory;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLException;

public class MimicTemporal {
    @Test
    public void test1(){

        String query =
                "PREFIX mt: <http://www.semanticweb.org/ontologies/2018/4/mimic/temporal/>\n" +
                        "PREFIX ms: <http://www.semanticweb.org/ontologies/2018/4/mimic/>" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "PREFIX time: <http://www.w3.org/2006/time#>" +
                        "SELECT ?p ?bInc ?b ?e ?eInc " +
                        "WHERE {" +
                        "GRAPH ?g {?p rdf:type mt:AdultICUPatientExcludedFromHIVClinicalTrial .}" +
                        "?g time:hasTime _:intv ." +
                        "_:inv time:isBeginInclusive ?bInc ." +
                        "_:intv time:hasBeginning _:beginInst ." +
                        "_:beginInst rdf:type time:Instant ." +
                        "_:beginInst time:inXSDDateTime ?b ." +
                        "_:intv time:hasEnd _:endInst ." +
                        "_:endInst rdf:type time:Instant ." +
                        "_:endInst time:inXSDDateTime ?e ." +
                        "_:inv time:isEndInclusive ?eInc . " +
                        "}";

        executeQuery(query);
    }

    @Test
    public void test2(){

        String query =
                "PREFIX mt: <http://www.semanticweb.org/ontologies/2018/4/mimic/temporal/>\n" +
                        "PREFIX ms: <http://www.semanticweb.org/ontologies/2018/4/mimic/>" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "PREFIX time: <http://www.w3.org/2006/time#>" +
                        "SELECT ?p ?bInc ?b ?e ?eInc " +
                        "WHERE {" +
                        "GRAPH ?g {?p rdf:type mt:ICUStay .}" +
                        "?g time:hasTime _:intv ." +
                        "_:inv time:isBeginInclusive ?bInc ." +
                        "_:intv time:hasBeginning _:beginInst ." +
                        "_:beginInst rdf:type time:Instant ." +
                        "_:beginInst time:inXSDDateTime ?b ." +
                        "_:intv time:hasEnd _:endInst ." +
                        "_:endInst rdf:type time:Instant ." +
                        "_:endInst time:inXSDDateTime ?e ." +
                        "_:inv time:isEndInclusive ?eInc . " +
                        "}";

        executeQuery(query);
    }

    @Test
    public void test3(){

        String query =
                "PREFIX mt: <http://www.semanticweb.org/ontologies/2018/4/mimic/temporal/>\n" +
                        "PREFIX ms: <http://www.semanticweb.org/ontologies/2018/4/mimic/>" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "PREFIX time: <http://www.w3.org/2006/time#>" +
                        "SELECT ?p ?bInc ?b ?e ?eInc " +
                        "WHERE {" +
                        "?p a ms:Patient ." +
                        "GRAPH ?g {?p mt:hasCreatinineLevel ?v.}" +
                        "?g time:hasTime _:intv ." +
                        "_:inv time:isBeginInclusive ?bInc ." +
                        "_:intv time:hasBeginning _:beginInst ." +
                        "_:beginInst rdf:type time:Instant ." +
                        "_:beginInst time:inXSDDateTime ?b ." +
                        "_:intv time:hasEnd _:endInst ." +
                        "_:endInst rdf:type time:Instant ." +
                        "_:endInst time:inXSDDateTime ?e ." +
                        "_:inv time:isEndInclusive ?eInc . " +
                        "}";

        executeQuery(query);
    }

    @Test
    public void test4(){

        String query =
                "PREFIX mt: <http://www.semanticweb.org/ontologies/2018/4/mimic/temporal/>\n" +
                        "PREFIX ms: <http://www.semanticweb.org/ontologies/2018/4/mimic/>" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "PREFIX time: <http://www.w3.org/2006/time#>" +
                        "SELECT ?p ?bInc ?b ?e ?eInc " +
                        "WHERE {" +
                        "GRAPH ?g {?p mt:hasBodyTemp ?v.}" +
                        "?g time:hasTime _:intv ." +
                        "_:inv time:isBeginInclusive ?bInc ." +
                        "_:intv time:hasBeginning _:beginInst ." +
                        "_:beginInst rdf:type time:Instant ." +
                        "_:beginInst time:inXSDDateTime ?b ." +
                        "_:intv time:hasEnd _:endInst ." +
                        "_:endInst rdf:type time:Instant ." +
                        "_:endInst time:inXSDDateTime ?e ." +
                        "_:inv time:isEndInclusive ?eInc . " +
                        "}";

        executeQuery(query);
    }

    @Test
    public void test5(){

        String query =
                "PREFIX mt: <http://www.semanticweb.org/ontologies/2018/4/mimic/temporal/>\n" +
                        "PREFIX ms: <http://www.semanticweb.org/ontologies/2018/4/mimic/>" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "PREFIX time: <http://www.w3.org/2006/time#>" +
                        "SELECT ?p ?bInc ?b ?e ?eInc " +
                        "WHERE {" +
                        "GRAPH ?g {?p rdf:type mt:ICUStayFirstDay.}" +
                        "?g time:hasTime _:intv ." +
                        "_:inv time:isBeginInclusive ?bInc ." +
                        "_:intv time:hasBeginning _:beginInst ." +
                        "_:beginInst rdf:type time:Instant ." +
                        "_:beginInst time:inXSDDateTime ?b ." +
                        "_:intv time:hasEnd _:endInst ." +
                        "_:endInst rdf:type time:Instant ." +
                        "_:endInst time:inXSDDateTime ?e ." +
                        "_:inv time:isEndInclusive ?eInc . " +
                        "}";

        executeQuery(query);
    }

    @Test
    public void test6(){

        String query =
                "PREFIX mt: <http://www.semanticweb.org/ontologies/2018/4/mimic/temporal/>\n" +
                        "PREFIX ms: <http://www.semanticweb.org/ontologies/2018/4/mimic/>" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "PREFIX time: <http://www.w3.org/2006/time#>" +
                        "PREFIX icd: <http://purl.bioontology.org/ontology/ICD9CM/>" +
                        "SELECT ?p ?bInc ?b ?e ?eInc " +
                        "WHERE {" +
                        "?p rdf:type icd:995.91 ." +
                        "GRAPH ?g {?p mt:hasFirstDayCreatinineLevel ?value.}" +
                        "?g time:hasTime _:intv ." +
                        "_:inv time:isBeginInclusive ?bInc ." +
                        "_:intv time:hasBeginning _:beginInst ." +
                        "_:beginInst rdf:type time:Instant ." +
                        "_:beginInst time:inXSDDateTime ?b ." +
                        "_:intv time:hasEnd _:endInst ." +
                        "_:endInst rdf:type time:Instant ." +
                        "_:endInst time:inXSDDateTime ?e ." +
                        "_:inv time:isEndInclusive ?eInc . " +
                        "}";

        executeQuery(query);
    }

    @Test
    public void test7(){

        String query =
                "PREFIX mt: <http://www.semanticweb.org/ontologies/2018/4/mimic/temporal/>\n" +
                        "PREFIX ms: <http://www.semanticweb.org/ontologies/2018/4/mimic/>" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "PREFIX time: <http://www.w3.org/2006/time#>" +
                        "SELECT ?p ?bInc ?b ?e ?eInc " +
                        "WHERE {" +
                        "GRAPH ?g {?p rdf:type mt:ArterialLineDressed.}" +
                        "?g time:hasTime _:intv ." +
                        "_:inv time:isBeginInclusive ?bInc ." +
                        "_:intv time:hasBeginning _:beginInst ." +
                        "_:beginInst rdf:type time:Instant ." +
                        "_:beginInst time:inXSDDateTime ?b ." +
                        "_:intv time:hasEnd _:endInst ." +
                        "_:endInst rdf:type time:Instant ." +
                        "_:endInst time:inXSDDateTime ?e ." +
                        "_:inv time:isEndInclusive ?eInc . " +
                        "}";

        executeQuery(query);
    }

    @Test
    public void test8(){

        String query =
                "PREFIX mt: <http://www.semanticweb.org/ontologies/2018/4/mimic/temporal/>\n" +
                        "PREFIX ms: <http://www.semanticweb.org/ontologies/2018/4/mimic/>" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "PREFIX time: <http://www.w3.org/2006/time#>" +
                        "SELECT ?p ?bInc ?b ?e ?eInc " +
                        "WHERE {" +
                        "GRAPH ?g {?p rdf:type mt:ICUPatientDressedArterialLineCatheter.}" +
                        "?g time:hasTime _:intv ." +
                        "_:inv time:isBeginInclusive ?bInc ." +
                        "_:intv time:hasBeginning _:beginInst ." +
                        "_:beginInst rdf:type time:Instant ." +
                        "_:beginInst time:inXSDDateTime ?b ." +
                        "_:intv time:hasEnd _:endInst ." +
                        "_:endInst rdf:type time:Instant ." +
                        "_:endInst time:inXSDDateTime ?e ." +
                        "_:inv time:isEndInclusive ?eInc . " +
                        "}";

        executeQuery(query);
    }

    @Test
    public void test9(){

        String query =
                "PREFIX mt: <http://www.semanticweb.org/ontologies/2018/4/mimic/temporal/>\n" +
                        "PREFIX ms: <http://www.semanticweb.org/ontologies/2018/4/mimic/>" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "PREFIX time: <http://www.w3.org/2006/time#>" +
                        "PREFIX icd: <http://purl.bioontology.org/ontology/ICD9CM/>\n" +
                        "SELECT ?id ?v ?l ?bInc ?b ?e ?eInc " +
                        "WHERE {" +
                        "GRAPH ?g {?p mt:hasFirstDayCreatinineLevel ?v.}" +
                        "?g time:hasTime _:intv ." +
                        "_:inv time:isBeginInclusive ?bInc ." +
                        "_:intv time:hasBeginning _:beginInst ." +
                        "_:beginInst rdf:type time:Instant ." +
                        "_:beginInst time:inXSDDateTime ?b ." +
                        "_:intv time:hasEnd _:endInst ." +
                        "_:endInst rdf:type time:Instant ." +
                        "_:endInst time:inXSDDateTime ?e ." +
                        "_:inv time:isEndInclusive ?eInc . " +
                        "?p ms:hasBeenDiagnosedWith ?d." +
                        "?p ms:hasPatientID ?id ." +
                        "?d ms:icd9Code ?cd ." +
                        "?d ms:icd9Class icd:995.91 ." +
                        "icd:995.91 rdfs:label ?l ." +
                        "}";

        executeQuery(query);
    }


    private void executeQuery(String query) {
        try {
            OntopOWLFactory factory = OntopOWLFactory.defaultFactory();

            OntopTemporalSQLOWLAPIConfiguration configuration = OntopTemporalSQLOWLAPIConfiguration.defaultBuilder()
                    .ontologyFile("src/test/resources/mimic.owl")
                    .nativeOntopMappingFile("src/test/resources/mimic.obda")
                    .nativeOntopTemporalMappingFile("src/test/resources/mimic.tobda")
                    .nativeOntopTemporalRuleFile("src/test/resources/mimic.dmtl")
                    .propertyFile("src/test/resources/mimic.properties")
                    .build();

            factory.createReasoner(configuration).getConnection().createStatement().executeSelectQuery(query);

        } catch (OWLException e) {
            e.printStackTrace();
        }
    }

}
