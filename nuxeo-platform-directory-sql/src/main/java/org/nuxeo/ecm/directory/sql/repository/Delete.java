/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 *
 * $Id: Delete.java 18043 2007-05-01 18:01:55Z fguillaume $
 */

package org.nuxeo.ecm.directory.sql.repository;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;

/**
 * A {@code DELETE} statement.
 *
 * @author Florent Guillaume
 */
public class Delete implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Dialect dialect;

    private Table table;

    private String where;

    public Delete(Dialect dialect) {
        this.dialect = dialect;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String getStatement() {
        StringBuilder buf = new StringBuilder(50);
        buf.append("delete from ");
        buf.append(table.getQuotedName(dialect));
        if (where != null && where.length() != 0) {
            buf.append(" where ");
            buf.append(where);
        }
        return buf.toString();
    }

}
