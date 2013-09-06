/*
 * Copyright (C) 2009-2013, Free University of Bozen Bolzano
 * This source code is available under the terms of the Affero General Public
 * License v3.
 * 
 * Please see LICENSE.txt for full license terms, including the availability of
 * proprietary exceptions.
 */
package it.unibz.krdb.obda.model;

import java.io.Serializable;

public interface OBDAModelListener extends Serializable {

	public void datasourceAdded(OBDADataSource source);

	public void datasourceDeleted(OBDADataSource source);

	public void datasourceUpdated(String oldname, OBDADataSource currendata);

	public void alldatasourcesDeleted();

	public void datasourcParametersUpdated();
}