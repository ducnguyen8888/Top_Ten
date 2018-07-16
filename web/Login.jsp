<%--
  Created by IntelliJ IDEA.
  User: Duc.Nguyen
  Date: 7/16/2018
  Time: 11:19 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page import="java.util.*, act.app.topten.*"
%><%@ include file="configuration.jsp"
%><jsp:useBean id="topTenUser" class="act.app.topten.TopTenUser" scope="session"
/><%
  String          userName                = request.getParameter("userName");
  String          pin                     = request.getParameter("pin");
  String          loginErrorMessage       ="";

  boolean         wasPosted               = "POST".equals(request.getMethod());
  String          ipAddress               = nvl( request.getHeader("X-Forwarded-For"), request.getRemoteAddr() );

  LoginLockout    lockout                 = null;
  boolean         isAccountOnLockDown     = false;

  String          dataSource              ="jdbc/development";

  try {
    topTenUser.invalidate();
    if( isDefined( userName )
      && isDefined( pin ) ) {
      Hashtable lockouts = (Hashtable) application.getAttribute("topTenLockouts");
      if ( lockouts == null ) {
        application.setAttribute( "topTenLockouts", (lockouts = new Hashtable()));
      }
      try {
        lockout = (LoginLockout) lockouts.get(ipAddress);
      } catch (ClassCastException classCast) {}

      if ( lockout == null ) {
        lockouts.put(ipAddress, (lockout = new LoginLockout()));
      } else if ( lockout.isLocked() ) {
        throw IP_ON_LOCKDOWN;
      } else if ( !lockout.allowLoginAttempt() ){
        throw IP_ON_LOCKDOWN;
      }

      topTenUser.setUser( userName, pin );
      topTenUser.validate( dataSource );

      if ( topTenUser.isSameRequestor( request )
              && topTenUser.isAllowedAccess( request )
              && wasPosted ) {
        if ( !topTenUser.isValid() ) { throw INVALID_USER; }
          session.setAttribute("topTenUser", topTenUser);
          lockouts.remove(ipAddress);
          %><html>
              <head>
                  <META HTTP-EQUIV="refresh" CONTENT="0;URL=main_master.jsp">
              </head>
                <body>
                  <div style="min-height: 400px; height: 400px;">Please wait...</div>
                </body>
            </html>
          <%
      }
    }

  } catch (Exception exception){
          if ( exception == IP_ON_LOCKDOWN ) {
            loginErrorMessage   = exception.getMessage();
            isAccountOnLockDown = true;
          } else if ( exception == INVALID_USER ) {
            loginErrorMessage   = exception.getMessage();
          } else {
            loginErrorMessage   = "Top ten login is currently unavailable, please try again later";
          }
  }

%><!DOCTYPE html>
<html>
  <head>
    <style>
      /* login fields
      --------------*/
      .group {
        margin-top: 25px;
        margin-bottom: 50px;
      }

      .input-group {
        font-family: Verdana,Helvetica,sans-serif; font-size:12px; font-weight: normal;
        margin-left: 510px;
        padding:10px 0px 10px 0px;

      }

      .input-group label {
        display: block;
        font-weight:bold;
      }


      .input-group input {
        display: block;
        width: 250px; padding: 5px;
        margin-top: 5px;
      }


      /* login button
      -------------*/
      .command {
        margin-left: 535px;
      }
      .command button {
        width: 200px; margin-left: 0px; margin-right: 0px;
      }


      /* error display
      --------------*/
      #error {
        color : red;
        text-align: center;
        margin-top: 30px;;
        margin-bottom: 10px;;
      }


      /* javascript warning display
      ---------------------------*/
      .overlay {
        display: block; position: fixed; z-index: 100; top: 0px; left: 0px; right: 0px; bottom: 0px;
      }

      .overlay .overlay-background {
        position: absolute; z-index: -1; top: 0px; left: 0px; right: 0px; bottom: 0px;
        background: black; opacity: 0.4;
      }

      .javascriptWarning {
        font-family: Verdana, Helvetica, sans-serif; font-size: 12px; font-weight: normal; color: black;
        background: white; width: 600px;
        margin: 200px auto 0px auto;
        padding: 1px 0px 20px 0px;
        border-radius: 6px;
        text-align: center;
      }

      .javascriptWarning div {
        margin-top: 25px;
        margin-bottom: 15px;
      }
      .javascriptWarning h6 {
        margin-top: 20px;
        color: red;
      }

      #warning div {
        margin-top: 10px;
        margin-bottom: 10px;
      }

    </style>
    <script>
      function validateLoginInput(){
        var userName = $("#userName").val();
        var pin      = $("#pin").val();

        if ( userName.length > 0 && pin.length > 0 ){
          $("#loginBtn").prop('disabled', false);
        } else {
          $("#loginBtn").prop('disabled', true);
        }
      }
    </script>
    <noscript>
      <div  class="overlay">
        <div class="overlay-background"></div>
        <div class="javascriptWarning">
          <h6>JavaScript Required</h6>
          <div id="warning">
            <div>It appears your web browser is not using JavaScript. Without it, some pages won't work properly.</div>
            <div>Please adjust the settings in your browser to make sure JavaScript is turned on.</div>
          </div>
        </div>
      </div>
    </noscript>
  </head>
  <body>
    <div id="error">
      <%= loginErrorMessage%>
    </div>
    <form action="Login.jsp" method="post">
      <div class="group">
        <div class="input-group">
          <label>
            Login Name
          </label>
          <div>
            <input type = "text" id="userName" name="userName"/>
          </div>
        </div>
        <div class="input-group">
          <label>
            Password
          </label>
          <input type="password" id="pin" name="pin" />
        </div>
      </div>
      <div class="command">
        <button type="submit" id="loginBtn"> Login</button>
      </div>
    </form>
  </body>
</html>
<%!
  Exception       IP_ON_LOCKDOWN              = new Exception("This account is currently locked due to excessive login attempts");
  Exception       INVALID_USER                = new Exception("An invalid login name or password was entered, please try again");

  public class LoginLockout {
    public LoginLockout() {}

    long    trackingDuration            = 5 * 60 * 1000;    // 5 minutes
    long    lockDuration                = 30 * 60 * 1000;   // 30 minutes
    int     maximumAttempts             = 7;

    long    lockedUntil                 = 0;

    long    loginAttemptsStarted        = (new java.util.Date()).getTime();
    int     attempts                    = 0;

    public boolean allowLoginAttempt() {
      if ( loginAttemptsStarted < (new java.util.Date()).getTime()-trackingDuration) {
        loginAttemptsStarted = (new java.util.Date()).getTime();
        attempts = 0;
      }

      if ( ++attempts > maximumAttempts ) {
        lockedUntil = (new java.util.Date()).getTime() + lockDuration;
      }
      return (attempts <= maximumAttempts);
    }
    public boolean isLocked() {
      if ( lockedUntil == 0 ) return false;
      if ( lockedUntil > (new java.util.Date()).getTime() ) return true;

      lockedUntil = 0;
      return false;
    }
  }

%>
