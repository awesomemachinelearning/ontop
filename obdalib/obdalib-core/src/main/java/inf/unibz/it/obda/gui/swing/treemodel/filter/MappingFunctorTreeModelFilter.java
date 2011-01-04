package inf.unibz.it.obda.gui.swing.treemodel.filter;

import inf.unibz.it.obda.domain.OBDAMappingAxiom;

import java.util.List;

import org.obda.query.domain.Atom;
import org.obda.query.domain.CQIE;
import org.obda.query.domain.Term;
import org.obda.query.domain.imp.CQIEImpl;
import org.obda.query.domain.imp.FunctionalTermImpl;
import org.obda.query.domain.imp.VariableImpl;

/**
 * @author This filter receives a string and returns true if any mapping
 *         contains the functor in some of the atoms in the head
 *
 */

public class MappingFunctorTreeModelFilter implements
		TreeModelFilter<OBDAMappingAxiom> {

	private String strMappingFunctor = "";

	/**
	 * @param strMappingFunctor
	 *            Constructor of the function
	 */
	public MappingFunctorTreeModelFilter(String strMappingFunctor) {
		this.strMappingFunctor = strMappingFunctor;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * inf.unibz.it.obda.gui.swing.treemodel.filter.TreeModelFilter#match(java
	 * .lang.Object)
	 */
	@Override
	public boolean match(OBDAMappingAxiom object) {
		boolean filterValue = false;
		OBDAMappingAxiom mapping = object;
		CQIE headquery = (CQIEImpl) mapping.getTargetQuery();
		List<Atom> atoms = headquery.getBody();

		for (int i = 0; i < atoms.size(); i++) {
			Atom atom = atoms.get(i);

			List<Term> queryTerms = atom.getTerms();

			for (int j = 0; j < queryTerms.size(); j++) {
				Term term = queryTerms.get(j);

				if (term instanceof FunctionalTermImpl) {
					FunctionalTermImpl functionTerm = (FunctionalTermImpl) term;
					if(functionTerm.getName().indexOf(strMappingFunctor)!= -1)
					{
						filterValue = true;
					}

				}

				if (term instanceof VariableImpl) {
					VariableImpl variableTerm = (VariableImpl) term;
					if(variableTerm.getName().indexOf(strMappingFunctor)!= -1)
					{
						filterValue = true;
					}

				}

				/*
				 * if(term.getName().indexOf(strMappingFunctor) != -1)
				 * filterValue = true; if (term instanceof VariableTerm) { if
				 * (term.toString().indexOf(strMappingFunctor) != -1)
				 * filterValue = true; } else
				 */

			}
		}

		return filterValue;
	}

}
