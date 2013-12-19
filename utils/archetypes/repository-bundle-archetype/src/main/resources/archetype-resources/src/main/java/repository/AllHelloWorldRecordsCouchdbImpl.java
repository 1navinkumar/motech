#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.repository;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.View;
import org.motechproject.commons.couchdb.dao.MotechBaseRepository;
import ${package}.domain.HelloWorldRecord;
import ${package}.domain.HelloWorldRecordCouchdbImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link AllHelloWorldRecords} uses CouchDB.
 */
@Component
@View(name = "all", map = "function(doc) { emit(doc._id, doc); }")
public class AllHelloWorldRecordsCouchdbImpl extends MotechBaseRepository<HelloWorldRecordCouchdbImpl> implements
        AllHelloWorldRecords {

    @Autowired
    protected AllHelloWorldRecordsCouchdbImpl(@Qualifier("helloWorldDbConnector") CouchDbConnector db) {
        super(HelloWorldRecordCouchdbImpl.class, db);
    }

    @Override
    public void add(HelloWorldRecord record) {
        if (findByRecordName(record.getName()) != null) {
            return;
        }
        super.add((HelloWorldRecordCouchdbImpl) record);
    }

    @Override
    @View(name = "by_recordName", map = "function(doc) { if (doc.type ==='HelloWorldRecord') { emit(doc.name, doc._id); }}")
    public HelloWorldRecord findByRecordName(String recordName) {
        if (recordName == null) {
            return null;
        }
        ViewQuery viewQuery = createQuery("by_recordName").key(recordName).includeDocs(true);
        return singleResult(db.queryView(viewQuery, HelloWorldRecordCouchdbImpl.class));
    }

    @Override
    public List<HelloWorldRecord> getRecords() {
        return new ArrayList<HelloWorldRecord>(getAll());
    }

    @Override
    public void delete(HelloWorldRecord record) {
        remove((HelloWorldRecordCouchdbImpl) record);
    }
}
