<%--
  Created by IntelliJ IDEA.
  User: Duc.Nguyen
  Date: 7/16/2018
  Time: 2:41 PM
  To change this template use File | Settings | File Templates.
--%>
<%!
    boolean isDefined( String val ) {
        return ( val != null && val.length() >0);
    }

    String nvl(String val, String alt) {
        return  isDefined( val ) ? val : alt;
    }
%>