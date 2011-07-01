package org.opendatakit.aggregate.odktables.entity;

import java.util.UUID;

import org.opendatakit.aggregate.odktables.relation.Rows;
import org.opendatakit.common.ermodel.Entity;
import org.opendatakit.common.ermodel.typedentity.TypedEntity;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.web.CallingContext;

/**
 * <p>
 * A Row is a set of values pertaining to the fields of a specific table.
 * </p>
 * 
 * <p>
 * Every Row has a revisionTag, which is a randomly generated uuid that should
 * be updated every time the Row is changed.
 * </p>
 * 
 * @author the.dylan.price@gmail.com
 */
public class Row extends TypedEntity
{

    public Row(String tableUUID, CallingContext cc) throws ODKDatastoreException
    {
        super(Rows.getInstance(tableUUID, cc));
    }

    public Row(Rows rows, Entity entity)
    {
        super(rows, entity);
    }

    public String getRevisionTag()
    {
        return super.getEntity().getField(Rows.REVISION_TAG);
    }

    public void updateRevisionTag()
    {
        super.getEntity().setField(Rows.REVISION_TAG,
                UUID.randomUUID().toString());
    }

    public String getValue(String fieldName)
    {
        return super.getEntity().getField(fieldName);
    }

    public void setValue(String fieldName, String value)
    {
        super.getEntity().setField(fieldName, value);
    }
}
