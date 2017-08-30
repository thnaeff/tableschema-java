package io.frictionlessdata.tableschema;

import io.frictionlessdata.tableschema.exceptions.InvalidCastException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * 
 */
public class Schema {
   
    private org.everit.json.schema.Schema tableJsonSchema = null;
    private List<Field> fields = new ArrayList();
    
    public Schema(){
        initValidator();
    }
    
    public Schema(JSONObject schema){
        initValidator(); 
        
        if(schema.has("fields")){
            Iterator iter = schema.getJSONArray("fields").iterator();
            while(iter.hasNext()){
                JSONObject fieldJsonObj = (JSONObject)iter.next();
                Field field = new Field(fieldJsonObj);
                this.fields.add(field);
            }
            
        }
    }
    
    public Schema(List<Field> fields){
        initValidator(); 
        this.fields = fields;
    }
    
    private void initValidator(){
        // Init for validation
        InputStream tableSchemaInputStream = TypeInferrer.class.getResourceAsStream("/schemas/table-schema.json");
        JSONObject rawTableJsonSchema = new JSONObject(new JSONTokener(tableSchemaInputStream));
        this.tableJsonSchema = SchemaLoader.load(rawTableJsonSchema);
    }
           
    
    public void addField(Field field){
        this.fields.add(field);
        
        try{
            this.tableJsonSchema.validate(this.getJson());         
            // No exception thrown? This means that the schema is valid.
        }catch(ValidationException ve){
            // If an Exception is thrown it means that the field that was justed added invalidates the schema.
            // We want to ignore this update on the scheme because now the updated version of the schema fails validation.
            // Simply remove last item that was added
            this.fields.remove(this.fields.size()-1);
        }
    }
    
    
    public void addField(JSONObject fieldJson){
        Field field = new Field(fieldJson);
        this.addField(field);
    }
    
    public List<Field> getFields(){
        return this.fields;
    }
    
    public Field getField(String name){
        Iterator<Field> iter = this.fields.iterator();
        while(iter.hasNext()){
            Field field = iter.next();
            if(field.getName().equalsIgnoreCase(name)){
                return field;
            }
        }
        return null;
    }
    
    public boolean validate(){
        try{
            this.tableJsonSchema.validate(this.getJson());
            return true;
        }catch(ValidationException ve){
            return false;
        }
    }
    
    public JSONObject getJson(){
        //FIXME: Maybe we should use JSON serializer like Gson?
        JSONObject schemaJson = new JSONObject();
        schemaJson.put("fields", new JSONArray());
        
        for(Field field : fields) {
            schemaJson.getJSONArray("fields").put(field.getJson());   
        }
        
        return schemaJson;
    }
    
    public Object[] castRow(String[] row) throws InvalidCastException{
        
        if(row.length != this.fields.size()){
            throw new InvalidCastException("Row length is not equal to the number of defined fields.");
        }
        
        try{
            Object[] castRow = new Object[this.fields.size()];
        
            for(int i=0; i<row.length; i++){
                Field field = this.fields.get(i);

                String castMethodName = "cast" + (field.getType().substring(0, 1).toUpperCase() + field.getType().substring(1));;
                Method method = TypeInferrer.class.getMethod(castMethodName, String.class, String.class);

                castRow[i] = method.invoke(new TypeInferrer(), field.getFormat(), row[i]);
            }

            return castRow;
            
        }catch(Exception e){
            throw new InvalidCastException();
        }
        
    }
   
}
