<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs['Language']}" />
<fmt:setBundle basename="templates.default"/>
<fmt:setBundle basename="KnowWE_messages" var="KnowWE" />
<div id="actionsTop" class="pageactions"> 
  <ul>

    <wiki:CheckRequestContext context='view|info|diff|upload'>
    <wiki:Permission permission="edit">
	<li>
        <wiki:PageType type="page">
          <a href="<wiki:EditLink format='url' />" accesskey="e"  class="action edit"
            title="<fmt:message key='actions.edit.title'/>" ><fmt:message key='actions.edit'/></a>
        </wiki:PageType>
        <wiki:PageType type="attachment">
          <a href="<wiki:BaseURL/>Edit.jsp?page=<wiki:ParentPageName />" accesskey="e" class="action edit"
            title="<fmt:message key='actions.editparent.title'/>" ><fmt:message key='actions.editparent'/></a>
        </wiki:PageType>
    </li>
    </wiki:Permission>
    </wiki:CheckRequestContext>

    <wiki:CheckRequestContext context='edit|comment'>
    <%-- converted to popup menu by jspwiki-edit.js--%>
    <li id="sectiontoc">
      <a href="#" class="action sectiontoc"><fmt:message key="edit.sections"/></a>
    </li>
    </wiki:CheckRequestContext>

    <%-- converted to popup menu by jspwiki-common.js--%>
    <li id="morebutton">
      <a href="<wiki:Link format='url' page='MoreMenu' />" class="action more"><fmt:message key="actions.more"/></a>
    </li>
    <li>
      <a class="action dialog" href='javascript:insertDialog()'><fmt:message bundle="${KnowWE}" key="actions.dialog"/></a>
    </li>
  </ul>
</div>