package act.app.topten;

/**
 * Created by Duc.Nguyen on 7/16/2018.
 */
import java.util.*;
import java.sql.*;

import javax.naming.InitialContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import javax.sql.DataSource;

public class TopTenUser {
    public TopTenUser() {

    }

    public TopTenUser(String user, String password) {
        setUser(user, password);
    }

    public TopTenUser(String dataSource, String user, String password) {
        setUser(user, password);
        validate(dataSource);
    }

    public TopTenUser(HttpServletRequest request, String dataSource, String user, String password) {
        this();
        setUser(user, password);
        register(request);
        validate(dataSource);
    }


    /** Returns a new class object
     *  @return TopTenUser returns a new class object
     */
    public static TopTenUser initialContext() { return new TopTenUser(); }


    /** Sets the user name and password to be used for this user
     *  <p>For security reasons any prior validation status or datasource will be cleared</p>
     *  @param user the user's database user name
     *  @param password  the user's database password
     *  @return PortfolioTaxOfficeUser returns this object, useful for chaining method calls
     */
    public TopTenUser setUser(String user, String password) {
        invalidate();
        this.dataSource = null;
        this.user       = user;
        this.password   = password;
        return this;
    }

    /** Validates the user against the database identified by the datasource name
     *  <p>The user id/password is used to attempt to log into the database</p>
     *  <p>User validation status is obtained by calling the isValid() method</p>
     *  @param dataSource the database data source of the database
     *  @return PortfolioTaxOfficeUser returns this object, useful for chaining method calls
     */
    public TopTenUser validate(String dataSource) {
        isValid = false;
        this.dataSource = dataSource;

        if ( isDefined(user) && isDefined(password) ) {
            try ( Connection con=open(dataSource, user, password); ) {
                try ( PreparedStatement ps = con.prepareStatement(
                        "select client_id from user_security "
                                + " where client_id=act_utilities.get_client_id() and username=user "
                                + "   and form_name=? "
                                + "   and allow = 'Y'"
                ); ) {
                    ps.setString(1,entitlement);

                    try ( ResultSet rs = ps.executeQuery(); ) {
                        if ( rs.next() ) {
                            isValid = true;
                            clientId = rs.getString("client_id");
                        }
                    }
                }
            } catch (Exception exception) {
                failureReason = exception;
            }
        } else {
            failureReason = new Exception("User credentials were not defined");
        }
        return this;
    }



    /** Identifies whether this user is valid or not
     *  @return boolean true if the user is valid, false if not
     */
    public boolean isValid() {
        return isValid;
    }

    /** Invalidates the user. Subsequent isValid() method calls will return false */
    public void invalidate() {
        isValid = false;
        clientId = null;
    }

    /** Returns the failure exception thrown if the user failed validation
     *  @returns Exception exception thrown on failed user validation, null otherwise
     */
    public Exception getFailureReason() { return failureReason; }

    /** Opens a new database connection using the registered user and datasource
     *  @returns Connection open database connection
     *  @throws Exception if an error occurs opening the connection or the user is not valid
     */
    public Connection getConnection() throws Exception {
        if ( ! isValid ) throw new SQLException("Invalid user");
        return open(dataSource, user, password);

    }





    /** Records the user's IP address, Host, and any forwarded-for header
     *  <p>Future requests can be verfied using the isSameRequestor() method</p>
     *  @param request JSP request object, used to retrieve IP address information
     *  @return PortfolioTaxOfficeUser returns this object, useful for chaining method calls
     */
    public TopTenUser register(HttpServletRequest request) {
        if ( request == null ) return null;
        this.remoteAddr     = request.getRemoteAddr();
        this.remoteHost     = request.getRemoteHost();
        this.forwardedFor   = request.getHeader("X-Forwarded-For");
        return this;
    }

    /** Returns whether the user's IP address, and any forwarded-for header is
     *  the same as what was recorded with the register() method
     *  <p>If the register() method wasn't previously called then this method returns true</p>
     *  @param request JSP request object, used to retrieve IP address information
     *  @return boolean true if the requestor IP is the same, false if not
     */
    public boolean isSameRequestor(HttpServletRequest request) {
        return request != null
                && (notDefined(remoteAddr)   || remoteAddr.equals(request.getRemoteAddr()))
                && (notDefined(forwardedFor) || forwardedFor.equals(request.getHeader("X-Forwarded-For")));
    }


    /** Returns whether the user is allowed to access the requested page
     *  <p>By default all access is allowed</p>
     *  @param request JSP request object, used to retrieve request information
     *  @return boolean true if the requested page is allowed to this user, false otherwise
     */
    public boolean isAllowedAccess(HttpServletRequest request) {
        return true; // default to all access
    }


    /** Database entitlement the user must be assigned to be valid */
    public              String      entitlement         = "WEB_TOP10";



    /** Database data source */
    protected           String      dataSource          = null;

    /** Client ID the user is associated with in the database */
    protected           String      clientId            = null;

    /** User's database username */
    public              String      user                = null;

    /** User's database password */
    public              String      password            = null;


    /** User's remote IP address */
    protected           String      remoteAddr          = null;
    /** User's remote Host name */
    protected           String      remoteHost          = null;
    /** User's IP address as defined in the forwarded for header */
    protected           String      forwardedFor        = null;


    /** Flag denoting whether the user is valid or not */
    protected           boolean     isValid             = false;

    /** Any exception thrown when the user is validated */
    public              Exception   failureReason       = null;



    /** Returns the user's user name
     *  @returns String the user name
     */
    public  String      getUser() { return user; }


    /** Returns the user's client ID that is associated with the specified user name
     *  <p>The returned client ID is only valid if the user has been successfully validated<p>
     *  @returns String the client ID assoicated with this user name
     */
    public  String      getClientId() { return clientId; }




    /** Returns whether the specified value is defined or not
     *  <p>A value is considered defined if it is non-null and has a length greather than 0</p>
     *  @param value String value to evaluate
     *  @return boolean true if value is defined, false otherwise
     */
    public static boolean isDefined(String value) { return value != null && value.length() > 0; }


    /** Returns whether the specified value is undefined or not
     *  <p>A value is considered undefined if it is null or has a length equal to 0</p>
     *  @param value String value to evaluate
     *  @return boolean true if value is undefined, false otherwise
     */
    public static boolean notDefined(String value) { return value == null || value.length() == 0; }


    /** Returns the first non-null value or an empty String ("") if all values are null
     *  @param values One or more string values to evaluate
     *  @return String first non-null value, an empty String is returned if all values are null
     */
    public static String nvl(String... values) {
        if ( values != null ) {
            for ( String value : values ) {
                if ( value != null ) return value;
            }
        }

        return "";
    }

    /** Returns the first defined value, or an empty String ("") if no values are defined
     *  <p>A defined value is one that is not null and has a length greater than 0</p>
     *  @param values One or more string values to evaluate
     *  @return String first non-null value with length greater than 0, an empty String if no values are defined
     */
    public static String ndef(String... values) {
        if ( values != null ) {
            for ( String value : values ) {
                if ( value != null && value.length() > 0 ) return value;
            }
        }

        return "";
    }



    /** User's access history
     *  <p>Entires are added through the request() and comment() methods</p>
     */
    public StringBuffer access = new StringBuffer();


    /** Returns user's access history
     *  <p>Entires are added through the request() and comment() methods</p>
     */
    public String getAccessLog() { return access.toString(); }


    /** Adds a comment to the user's access log
     *  @param comment comment to add to the access log
     */
    public void comment(String comment) {
        if ( comment == null ) return;
        access.append(String.format("%s\n%s\n",
                (new java.util.Date()).toString(),
                comment
                )
        );
    }


    /** Records the user's requested page in the access log
     *  @param request JSP request object, used to retrieve requested page
     */
    public void request(HttpServletRequest request) {
        if ( request == null ) return;
        access.append(String.format("%s\nPage: %s\n",
                (new java.util.Date()).toString(),
                request.getServletPath()
                )
        );
    }



    /** Opens a new database connection to the specified data source connecting as the specified user.
     * @param dataSource a jdbc data source or JNDI named data source
     * @param user user to connect as
     * @param password user's password to connect
     * @return a new Connection to the database specified by data source
     * @throws Exception if an error occurs opening the connection
     */
    public static Connection open(String dataSource, String user, String password) throws Exception {
        Connection     connection      = null;

        if ( dataSource == null ) throw new SQLException("Data source not specified");

        if ( dataSource.startsWith("jdbc:") ) {
            connection = openURL(dataSource, user, password);
        } else {
            connection = (isDefined(user) ? openURL(getDatabaseURL(dataSource), user, password)
                    : openJNDI(dataSource)
            );
        }

        return connection;
    }

    /** Returns the database URL for a specified dataSource
     * @param dataSource a JNDI named data source
     * @return the database URL of the dataSource
     * @throws Exception if an error occurs retrieving the URL
     */
    public static String getDatabaseURL(String dataSource) throws Exception {
        if ( dataSource == null || dataSource.startsWith("jdbc:") ) return dataSource;

        String databaseURL = null;
        try ( Connection con=openJNDI(dataSource) ) {
            databaseURL = con.getMetaData().getURL();
        }

        return databaseURL;
    }

    /** Opens a new database connection to the specified JNDI data source
     * @param dataSource a JNDI named data source
     * @return a new Connection to the database specified by data source
     * @throws Exception if an error occurs opening the connection
     */
    public static Connection openJNDI(String dataSource) throws Exception {
        if ( dataSource == null ) throw new SQLException("Data source not specified");

        // Due to issues with how WebLogic handles connections we do not want to
        // specify a username/password when using a JNDI dataSource directly. These
        // connections are pooled and will cause problems if a user is specified.
        //
        // WebLogic ignores any specified username/password if a connection
        // is already available in the connection pool. If a connection is not
        // already open then WebLogic will open a new connection using the
        // specified username/password and then return that connection to the
        // general use pool when done. This creates a security issue where the
        // expected login user is not the actual login user that is connected.
        //
        // Since WebLogic ignores the specified username/password if a connection
        // is already available we can't rely on the creation of a connection to
        // validate provided database user credentials.

        return ((DataSource) (new InitialContext()).lookup(dataSource)).getConnection();
    }

    /** Opens a new database connection to the specified database URL as the specified user.
     * @param databaseURL a jdbc database URL (i.e. jdbc:oracle:thin:@ares:1521:actd)
     * @param user user to connect as
     * @param password user's password to connect
     * @return a new Connection to the database specified by data source
     * @throws Exception if an error occurs opening the connection
     */
    public static Connection openURL(String databaseURL, String user, String password) throws Exception {
        if ( databaseURL == null ) throw new SQLException("Database URL not specified");
        if ( user == null || password == null ) throw new SQLException("User not specified");

        // Note from Oracle OTN WebLogic thread
        // https://community.oracle.com/thread/688456?start=0&tstart=0
        //
        // Avoid calling DriverManager in a multithreaded application.
        // That call is class synchronized, as are several other DriverManager
        // calls that JDBC drivers and SQLExceptions call internally, often,
        // so one long-running getConnection() call can halt all other JDBC in the
        // JVM. Call Driver.connect() directly. That's what DriverManager does anyway
        //
        // try {
        // 	Class.forName("oracle.jdbc.driver.OracleDriver");
        // } catch ( ClassNotFoundException e ) {
        // 	throw new SQLException("Unable to load database driver");
        // }
        //
        // connection = java.sql.DriverManager.getConnection(dataSource, user, password);
        Driver driver = (Driver)(Class.forName("oracle.jdbc.OracleDriver").newInstance());
        Properties props = new Properties();
        props.put("user", user);
        props.put("password", password);
        return driver.connect(databaseURL, props);
    }
}
