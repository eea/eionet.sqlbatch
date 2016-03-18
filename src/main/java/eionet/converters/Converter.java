package eionet.converters;

import java.sql.SQLException;

/**
 * @author George Sofianos
 */
public interface Converter {

    void convert() throws ConvertException, SQLException;

    void setUrl(String s);

    void setInput(String arg);

}
