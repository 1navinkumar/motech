package repository.bundle.service;

import repository.bundle.domain.HelloWorldRecord;
import repository.bundle.domain.HelloWorldRecordCouchdbImpl;
import repository.bundle.domain.HelloWorldRecordDto;
import repository.bundle.repository.AllHelloWorldRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link HelloWorldRecordService} interface. Uses
 * {@link AllHelloWorldRecords} in order to retrieve and persist records.
 */
@Service("helloWorldRecordService")
public class HelloWorldRecordServiceImpl implements HelloWorldRecordService {

    @Autowired
    private AllHelloWorldRecords allHelloWorldRecords;

    @Override
    public void add(HelloWorldRecordDto record) {
        allHelloWorldRecords.add(new HelloWorldRecordCouchdbImpl(record.getName(), record.getMessage()));
    }

    @Override
    public HelloWorldRecordDto findByRecordName(String recordName) {
        HelloWorldRecord record = allHelloWorldRecords.findByRecordName(recordName);
        if (null == record) {
            return null;
        }
        return new HelloWorldRecordDto(record);
    }

    @Override
    public List<HelloWorldRecordDto> getRecords() {
        List<HelloWorldRecordDto> records = new ArrayList<>();
        for (HelloWorldRecord record : allHelloWorldRecords.getRecords()) {
            records.add(new HelloWorldRecordDto(record));
        }
        return records;
    }

    @Override
    public void delete(HelloWorldRecordDto record) {
        HelloWorldRecord exsitingRecord = allHelloWorldRecords.findByRecordName(record.getName());
        if (exsitingRecord != null) {
            allHelloWorldRecords.delete(exsitingRecord);
        }
    }
}