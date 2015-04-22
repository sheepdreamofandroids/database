/**
Copyright (C) SYSTAP, LLC 2006-2015.  All rights reserved.

Contact:
     SYSTAP, LLC
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@systap.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Sep 16, 2009
 */

package com.bigdata.rdf.sail;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryResult;

import com.bigdata.rdf.axioms.NoAxioms;
import com.bigdata.rdf.changesets.IChangeRecord;
import com.bigdata.rdf.internal.XSD;
import com.bigdata.rdf.model.BigdataBNode;
import com.bigdata.rdf.model.BigdataStatement;
import com.bigdata.rdf.model.BigdataValueFactory;
import com.bigdata.rdf.store.AbstractTripleStore;

/**
 * Test suite {@link RDRHistory}.
 */
public class TestRDRHistory extends ProxyBigdataSailTestCase {

    private static final Logger log = Logger.getLogger(TestRDRHistory.class);
    
    public Properties getProperties() {
        
        return getProperties(MyRDRHistory.class);
        
    }

    public Properties getProperties(final Class<? extends RDRHistory> cls) {
        
        Properties props = super.getProperties();
        
        // no inference
        props.setProperty(BigdataSail.Options.TRUTH_MAINTENANCE, "false");
        props.setProperty(BigdataSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        props.setProperty(BigdataSail.Options.JUSTIFY, "false");
        props.setProperty(BigdataSail.Options.TEXT_INDEX, "false");
        
        // turn on RDR history
        props.setProperty(AbstractTripleStore.Options.RDR_HISTORY_CLASS, cls.getName());
        
        return props;
        
    }

    /**
     * 
     */
    public TestRDRHistory() {
    }

    /**
     * @param arg0
     */
    public TestRDRHistory(String arg0) {
        super(arg0);
    }

    
    /**
     * Test basic add/remove.
     */
    public void testAddAndRemove() throws Exception {

        BigdataSailRepositoryConnection cxn = null;

        final BigdataSail sail = getSail(getProperties());

        try {

            sail.initialize();
            final BigdataSailRepository repo = new BigdataSailRepository(sail);
            cxn = (BigdataSailRepositoryConnection) repo.getConnection();

            final BigdataValueFactory vf = (BigdataValueFactory) sail
                  .getValueFactory();
            final URI s = vf.createURI(":s");
            final URI p = vf.createURI(":p");
            final URI o = vf.createURI(":o");

            BigdataStatement stmt = vf.createStatement(s, p, o);
            cxn.add(stmt);
            cxn.commit();
            
            if (log.isInfoEnabled()) {
                log.info(cxn.getTripleStore().dumpStore().insert(0,'\n'));
            }
            
            assertTrue(cxn.size() == 2);

            {
                RepositoryResult<Statement> stmts = cxn.getStatements(
                        s, p, o, false);
                assertTrue(stmts.hasNext());
                stmts.close();
            }

            {
                BigdataBNode sid = vf.createBNode(stmt);
                RepositoryResult<Statement> stmts = cxn.getStatements(
                        sid, MyRDRHistory.ADDED, null, false);
                assertTrue(stmts.hasNext());
                Literal l = (Literal) stmts.next().getObject();
                assertTrue(l.getDatatype().equals(XSD.DATETIME));
                stmts.close();
            }
            
            cxn.remove(stmt);
            cxn.commit();

            if (log.isInfoEnabled()) {
                log.info(cxn.getTripleStore().dumpStore().insert(0,'\n'));
            }
            
            assertTrue(cxn.size() == 2);

            {
                RepositoryResult<Statement> stmts = cxn.getStatements(
                        s, p, o, false);
                assertFalse(stmts.hasNext());
                stmts.close();
            }

            {
                BigdataBNode sid = vf.createBNode(stmt);
                RepositoryResult<Statement> stmts = cxn.getStatements(
                        sid, MyRDRHistory.REMOVED, null, false);
                assertTrue(stmts.hasNext());
                Literal l = (Literal) stmts.next().getObject();
                assertTrue(l.getDatatype().equals(XSD.DATETIME));
                stmts.close();
            }
            
        } finally {
            if (cxn != null)
                cxn.close();
            
            sail.__tearDownUnitTest();
        }
    }
    
    /**
     * Test custom history handler.
     */
    public void testCustomHistory() throws Exception {

        BigdataSailRepositoryConnection cxn = null;

        final BigdataSail sail = getSail(getProperties(CustomRDRHistory.class));

        try {

            sail.initialize();
            final BigdataSailRepository repo = new BigdataSailRepository(sail);
            cxn = (BigdataSailRepositoryConnection) repo.getConnection();

            final BigdataValueFactory vf = (BigdataValueFactory) sail
                  .getValueFactory();
            final URI s = vf.createURI(":s");
            final URI p = vf.createURI(":p");
            final URI o = vf.createURI(":o");
            final Literal l = vf.createLiteral("o");

            BigdataStatement stmt1 = vf.createStatement(s, p, o);
            BigdataStatement stmt2 = vf.createStatement(s, p, l);
            cxn.add(stmt1);
            cxn.add(stmt2);
            cxn.commit();
            
            if (log.isInfoEnabled()) {
                log.info(cxn.getTripleStore().dumpStore().insert(0,'\n'));
            }
            
            assertTrue(cxn.size() == 3);

            {
                RepositoryResult<Statement> stmts = cxn.getStatements(
                        s, p, o, false);
                assertTrue(stmts.hasNext());
                stmts.close();
            }

            {
                RepositoryResult<Statement> stmts = cxn.getStatements(
                        s, p, l, false);
                assertTrue(stmts.hasNext());
                stmts.close();
            }

            {
                BigdataBNode sid = vf.createBNode(stmt1);
                RepositoryResult<Statement> stmts = cxn.getStatements(
                        sid, MyRDRHistory.ADDED, null, false);
                assertFalse(stmts.hasNext());
                stmts.close();
            }
            
            {
                BigdataBNode sid = vf.createBNode(stmt2);
                RepositoryResult<Statement> stmts = cxn.getStatements(
                        sid, MyRDRHistory.ADDED, null, false);
                assertTrue(stmts.hasNext());
                Literal l2 = (Literal) stmts.next().getObject();
                assertTrue(l2.getDatatype().equals(XSD.DATETIME));
                stmts.close();
            }
            
        } finally {
            if (cxn != null)
                cxn.close();
            
            sail.__tearDownUnitTest();
        }
    }
    
    public static class MyRDRHistory extends RDRHistory {

        public static final URI ADDED = new URIImpl(":added");
        
        public static final URI REMOVED = new URIImpl(":removed");
        
        public MyRDRHistory(final AbstractTripleStore database) {
            super(database);
        }

        @Override
        protected URI added() {
            return ADDED;
        }

        @Override
        protected URI removed() {
            return REMOVED;
        }
        
    }

    public static class CustomRDRHistory extends MyRDRHistory {

        public CustomRDRHistory(AbstractTripleStore database) {
            super(database);
        }

        /**
         * Only accept stmts where isLiteral(stmt.o)
         */
        @Override
        protected boolean accept(final IChangeRecord record) {
            return record.getStatement().o().isLiteral();
        }
        
    }
    
}
