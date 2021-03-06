package it.unibz.inf.ontop.substitution.impl;

/*
 * #%L
 * ontop-reformulation-core
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.substitution.Substitution;

import java.util.List;

/**
 * This utility class provides common operations
 * on substitution functions.
 *
 */
public class SubstitutionUtilities {

    /**
     * Applies the substitution to all the terms in the list. Note that this
     * will not clone the list or the terms inside the list.
     *
     * @param atom
     * @param unifier
     */

    public static void applySubstitution(Function atom, Substitution unifier) {
        applySubstitution(atom, unifier, 0);
    }

    public static void applySubstitution(Function atom, Substitution unifier, int fromIndex) {

        List<Term> terms = atom.getTerms();

        for (int i = fromIndex; i < terms.size(); i++) {
            Term t = terms.get(i);

            // unifiers only apply to variables, simple or inside functional terms

            if (t instanceof Variable) {
                Term replacement = unifier.get((Variable) t);
                if (replacement != null)
                    terms.set(i, replacement);
            } 
            else if (t instanceof Function) {
                applySubstitution((Function) t, unifier);
            }
        }
    }



}
