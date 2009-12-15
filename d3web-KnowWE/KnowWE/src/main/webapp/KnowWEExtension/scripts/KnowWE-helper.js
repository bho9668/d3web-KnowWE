/**
 * Title: KnowWE-helper
 * Some helper functions used in the javascript files in KnowWE. Was separated
 * from the core KnowWE javascript functions for easier reuse and clarity.
 */

if (typeof KNOWWE == "undefined" || !KNOWWE) {
    /**
     * The KNOWWE global namespace object.  If KNOWWE is already defined, the
     * existing KNOWWE object will not be overwritten so that defined
     * namespaces are preserved.
     */
    var KNOWWE = {};
}

/**
 * Class: KNOWWE.helper
 * Some helper functions packed in the KNOWWE.helper namespace.
 * This was separated from the main KNOWWE due clarity.
 */
KNOWWE.helper = function(){
    return {
        /**
         * Function: containsArr
         * Checks if a value is already in a array.
         * 
         * Parameters:
         *     a - The array that maybe contains the value.
         *     obj -  The element that should be checked for.
         * 
         * Returns:
         *     TRUE if the value is found, otherwise FALSE.
         */
        containsArr : function(a, obj){
            for(var i = 0; i < a.length; i++) {
                if(a[i] === obj){
                    return true;
                }
            } 
            return false;
        },
        /**
         * Function: removeArr
         * Removes the given value from the given array.
         * 
         * Parameters:
         *     a - The array the value should removed from.
         *     obj -  The element that should be removed.
         * 
         * Returns:
         *     The shrunk array.
         */
        removeArr : function(a, obj){
            for(var i = 0; i < a.length; i++) {
                if( a[i] === obj ){
                    var r = a.slice(i + 1);
                    a.length = i;
                    if(r.length > 0)
                        return a.push.apply( a, r );
                }
            }
            return a;
        },
        /**
         * Function: enrich
         * Enriches an object by replacing its key:value pairs with those from an other
         * object. Also non existing key:value pairs from the first object are added. Key:value
         * pairs that occur not in the oNew object are not changed in the 
         * oDefault object.
         * 
         * Parameters:
         *     oNew -  The array with new or additional key:value pairs.
         *     oDefault -  The array with the default key:value pairs.
         * 
         * Returns:
         *     The enriched array.
         */
        enrich : function (oNew, oDefault){
            if( typeof(oNew) != 'undefined' && oNew != null){
                for( var i in oNew ) {
                    if( oNew[i] != null && typeof oNew[i] != 'object' ) oDefault[i] = oNew[i];
                    if(typeof oNew[i] == 'object') this.enrich( oNew[i], oDefault[i]);
                }
            }
            return oDefault;
        },
        /**
         * Function: getXMouse
         * Returns to a given event the current x position in the browser window.
         * 
         * Parameters:
         *     e - The occurred event.
         * 
         * Returns:
         *     The mouseX position
         */        
        getXMouse : function(e){
            e = e || window.event;      
            return e.pageX || e.clientX + document.body.scrollLeft; 
        },
        /**
         * Function: getYMouse
         * Returns to a given event the current y position in the browser window.
         * 
         * Parameters:
         *     e - The occurred event.
         * 
         * Returns:
         *     The mouseY position
         */   
        getYMouse : function(e){
            e = e || window.event;
            return e.pageY || e.clientY + document.body.scrollTop;
        },
        /**
         * Function: findXY
         * Returns the distance from the top left corner of the document of a
         * DOM element. Inspired by Peter-Paul Koch
         *   
         * Parameters:       
         *      obj - The DOM element one wants to know the distance.
         * 
         *  Returns:
         *     The x and y value of the distance.
         */
        findXY : function( e ) {
            var left = 0;
            var top  = 0;
        
            while (e.offsetParent){
                left += e.offsetLeft;
                top  += e.offsetTop;
                e     = e.offsetParent;
            }
        
            left += e.offsetLeft;
            top  += e.offsetTop;
        
            return {x:left, y:top};
        },   
        /**
         * Function: formatAttributes
         * Formats the attribute node of an DOM element.
         * 
         * Parameters:
         *     attr - The attribute node of the DOM element.
         * 
         * Returns:
         *     The attributes of the DOM element as string
         */
        formatAttributes : function( attr ){
            var s ='', i = 0;
            for( i = 0; i < attr.length; i++){
                var nodeName = attr[i].name; 
                if(nodeName.startsWith('_')) continue; //due the KNOWWE element class
                s += attr[i].nodeValue + '#';
            }
            return s;
        },        
        /**
         * Function: getMouseOffset
         * 
         * Parameters:
         *     target -
         *     e - 
         * 
         * Returns:
         * 
         */
        getMouseOffset : function(target, e){
            e = e || window.event;

            var docPos    = KNOWWE.helper.findXY(target);
            var mousePos  = KNOWWE.helper.mouseCoords(e);
            return {x:mousePos.x - docPos.x, y:mousePos.y - docPos.y};
        },
        /**
         * Function: mouseCoords
         * Returns the position of the mouse cursor in the browser.
         * 
         * Parameters:
         *     e - An event.
         * 
         * Returns:
         *     The coords of the mouse cursor as array
         */
        mouseCoords : function( e ){
            if(e.pageX || e.pageY){
                return {x:e.pageX, y:e.pageY};
            }
            return {
                x:e.clientX + document.body.scrollLeft - document.body.clientLeft,
                y:e.clientY + document.body.scrollTop  - document.body.clientTop
            };
        },
        /**
         * Function: gup
         * Returns the value of a URL parameter. Which parameter is specified 
         * through the {@link name} parameter.
         * (start code)
         * ../Wiki.jsp?page=KnowWE-ExamplePage : 
         *     KNOWWE.util.gup( 'page' ) --> KnowWE-Example-Page 
         * (end)
         * 
         * Parameters:
         *     name - The parameter to search for in the URL
         * 
         * Returns:
         *     The found URL parameter value.
         */
        gup : function( name ){
            if(name.constructor !== String) return false;
            
            name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
            var regex = new RegExp( "[\\?&]" + name + "=([^&#]*)" );
            var results = regex.exec( window.location.href );
            if( results ) { 
                return decodeURIComponent(results[1]);
            }
            if(name === 'page') { //fix for url parameter topic on Main page  
                return 'Main';
            }
            return null;
        },
        /**
         * Function: loadCheck
         * Checks if the current page allows certain onload events.
         * If the pages parameter contains the current page, the onload 
         * event is triggered, otherwise none. Prevents errors due incorrect 
         * context for onload events.
         * 
         * Parameters:
         *     pages - A list of pages that should be checked.
         * 
         * Returns:
         *     The result of the check.
         */
         loadCheck : function( pages ){
            if(!pages || pages.constructor !== Array) return false;
            
            var path = window.location.pathname;
            //quick fix for checking if init actions should apply to start page
            if(path == '/KnowWE/') return true; 
            
            var path = path.split('/');
            var page = path[path.length - 1];
              
            for(var i = 0; i < pages.length; i++){
                if(page === pages[i])
                    return true;
            }
            return false;
        }
    }
}();

/**
 * Class: KNOWWE.helper.event
 * The KNOWWE event namespace. Used to add and remove events to certain DOM elements.
 * This is based on the mootools framework and simply wraps the event functions.
 */
KNOWWE.helper.event = function(){
    return {
        /**
         * Function: add
         * Adds an event to an object. If the eventType occurs the given function
         * is executed.
         * 
         * Parameters:
         * 
         *     eventType - The type of the event e.g. click, mouseout, ...
         *     object - The DomElement to which the event should be bind
         *     handler - The function that should be executed on the event
         */
        add : function(eventType, object, handler){
            //if(!object.addEvent) return;
            $(object).addEvent( eventType, handler ); /* based on mootools */
        },
        /**
         * Function: remove
         * Removes an event from an object.
         * 
         * Parameters:
         *     eventType - The type of the event e.g. click, mouseout, ...
         *     object - The DomElement to which the event should be bind
         *     handler - The function that should be executed on the event
         */
        remove : function(eventType, object, handler){
            $(object).removeEvent( eventType, handler ); /* based on mootools */
            /*object['on' + eventType] = null;*/
        },
        /**
         * Function: removeEvents
         * Removes all events from an object.
         * 
         * Parameters:
         *     eventType - The type of the event e.g. click, mouseout, ...
         *     object - The DomElement to which the event should be bind
         */        
        removeEvents : function( eventType, object ){
            $(object).removeEvents(eventType);
        },
        /**
         * Function: target
         * Returns the element which throw the event.
         * 
         * Parameters:
         *     e - The occurred event.
         * 
         * Returns:
         *     The element which triggered the element.
         */
        target : function( e ){
            e = e || window.event;
            return e.target || e.srcElement ;
        },
        cancel : function( e ){
            e.cancelBubble = true;
            if (e.stopPropagation) e.stopPropagation();
        }
    }
}();

/**
 * Class: KNOWWE.helper.ajax
 * This class is used to handle AJAX request in KNOWWE.
 * For possible content of the options object please read the comments in the 
 * source code.
 * 
 * Parameters:
 *     options - The options for the request.
 */
KNOWWE.helper.ajax = function ( options ) {
    var oDefault = {
        method : 'POST',
        url : 'KnowWE.jsp',
        headers : {
            'X-Requested-With': 'XMLHttpRequest',
            'Accept': 'text/javascript, text/html, application/xml, text/xml'
        },
        loader : false,
        fn : handleResponse,
        encoding : 'utf-8',
        urlEncoded : true,      
        async : true,
        response : {
            ids : [],          /* id used to handle an action in the response */
            action : 'insert', /* action triggered after the response is received
                                * the action value can be one of the following
                                * predefined values:
                                * - none: no action is triggered
                                * - replace: the content of the given ids is replaced with the response
                                * - insert: the response is inserted into the given ids
                                * - create: the response is inserted into the DOM before the element specified by create options
                                * - string: a special command: see comments below for explanation
                                */
            fn : false  
        },
        create : {
            id : '',
            fn : false
        }
    }
    var http = (window.XMLHttpRequest) ? new XMLHttpRequest() : new ActiveXObject('Microsoft.XMLHTTP');
    
    init();
    
    /**
     * Function: init
     * Initializes the some KNOWWE.ajax things
     */
    function init(){
        oDefault = KNOWWE.helper.enrich( options, oDefault );
    }
    
    /**
     * Function: createLoader
     * Creates an AJAX loading image.
     */
    function createLoader(){
        var loading = document.createElement('div');
        var domID = document.createAttribute('id');
        domID.nodeValue = 'KNOWWE-ajax-loader';
        loading.setAttributeNode( domID );
        loading.innerHTML = "<img src=\"KnowWEExtension/images/ajax-loader.gif\" width=\"32\" height=\"32\"/>";
        
        document.getElementsByTagName("body")[0].appendChild( loading );        
    }
    
    /** 
     * Function: handleResponse
     * Handles the response from the AJAX request. If the AJAX request ended 
     * without errors the action defined in oDefault.response.action is executed.
     */
    function handleResponse() {
        if ((http.readyState == 4) && (http.status == 200)) {
            var ids = oDefault.response.ids;
            var action = oDefault.response.action;
            
            switch ( action ) {
                case 'insert':
                    var max = ids.length;
                    for ( var i = 0; i < max; i++ ) {
                        document.getElementById(ids[i]).innerHTML = http.responseText;
                    }
                    break;
                case 'create':
                    if( oDefault.create ){
                        var el = oDefault.create.fn.call();
                        el.innerHTML = http.responseText;
                        document.getElementById( oDefault.create.id ).insertBefore( el, document.getElementById( oDefault.create.id ).childNodes[0]);
                    } 
                    break;
                case 'string':
                    if( http.responseText.startsWith('@info@')){
                         var info = new _KN('p', { 'class' : 'box info' });
                         info._setHTML( http.responseText.replace(/@info@/, '') );
                         info._injectTop(document.getElementById( ids[0]));
                    } 
                    if( http.responseText.startsWith('@replace@')){
                        var html = http.responseText.replace(/@replace@/, '');
                        KNOWWE.core.util.replace( ids, html );    
                    }
                    break;
                default:
                    break;
            }
            if( !oDefault.response.fn ){ return; }
            oDefault.response.fn.call();
            
            if(oDefault.loader) {
                var loader = document.getElementById('KNOWWE-ajax-loader');
                loader.parentNode.removeChild( loader );
            }
        }  
    }    
    
    /**
     * Function: send
     * Sends the AJAX request. 
     * Also specifies what to to after the response got back.
     */
    this.send = function() {
        if( !http ) return;

        var headers = new KNOWWE.helper.hash(oDefault.headers);
        if (oDefault.urlEncoded){
            var encoding = (oDefault.encoding) ? '; charset=' + oDefault.encoding : '';
            headers.set('Content-type', '"application/x-www-form-urlencoded' + encoding + '"');
        }       
        
        http.open( oDefault.method.toUpperCase(), oDefault.url, oDefault.async );
        
        headers.forEach( function(k, v){
            http.setRequestHeader(k, v);
        }); 
        
        http.onreadystatechange = oDefault.fn;       
        http.send(oDefault.method);
        if(oDefault.loader){
            createLoader();
        }
    }
    /**
     * Function: getResponse
     * Returns the text of the AJAX response.
     * 
     * Returns:
     *     Result of the response.
     */
    this.getResponse = function() {
        return http.responseText;
    }  
}

/**
 * Class: KNOWWE.helper.element
 * The KNOWWE element namespace.
 * Custom Native to allow all of its methods to be used with any extended DOM Element.
 * 
 * Parameters:
 *     tag - The tag of the element.
 *     properties - The properties of the element
 * 
 * Returns:
 *     A DOM element enriched with some methods
 */
KNOWWE.helper.element = function ( tag, properties ){
    var o;
    if(typeof tag === 'object' || typeof tag === 'array') {  /* assume DOM element should be wrapped with KNOWWE element functions.*/
        o = tag;
    } else {   /* assume tag and properties are present, create new element */
        if(tag.constructor !== String) {
            o = new Object();
        } else {
            o =  document.createElement( tag );
        }
        
        if(properties)
            o = this._setProperties( properties, o);
    }
    return enrich(o, KNOWWE.helper.element);
    
    function enrich(o1, o2){
        if(o1 == null || o2 == null) return o1;
        for( var i in o2.prototype ){
            o1[i] = o2.prototype[i];
        }
        return o1;
    }
}

/**
 * The KNOWWE element functions.
 * Used to extend the functionality of the DOM elements.
 */
KNOWWE.helper.element.prototype = {
    /**
     * Function: _clear
     * Clears the innerHTML of the current element.
     */
    _clear : function(){
        this.innerHTML = '';
    },
    /**
     * Function: _css
     * Sets the given CSS properties to the current element. The properties
     * should be given as an object.
     * 
     * Parameters:
     *     properties - The CSS properties to be applied to the element. 
     */
    _css : function( properties ){
        if(properties.constructor !== Object) throw new Error('awaits object');
        for( var i in properties){
            this.style[i] = properties[i];
        }
    },
    /**
     * Function: _next
     * Returns the next element on the same level as the current element. The 
     * elements are therefore neighbours.
     * 
     * Returns:
     *     The next DOM element.
     */
    _next : function() {
        var elem = this;
        do {
            elem = elem.nextSibling;
        } while ( elem && elem.nodeType != 1 );
        return new KNOWWE.helper.element( elem );
    },
    /**
     * Function: _previous
     * Returns the previous element on the same level as the current element. The 
     * elements are therefore neighbours.
     * 
     * Returns:
     *     The previous DOM element.
     */    
    _previous : function(){
        var e = this;
        do  {
            e = e.previousSibling;
        } while( e && e.nodeType != -1);
        return  KNOWWE.helper.element( e );
    },
    /**
     * Function: _getChildren
     * Returns the children of the current element.
     * 
     * Returns:
     *     The children of the current DOM element.
     */    
    _getChildren : function(){
        return this.childNodes;
    },
     /**
     * Function: _getParent
     * Returns the parent node of the current DOM element.
     * 
     * Returns:
     *     Parent node of the current element.
     */      
    _getParent : function( clazz ){
        var e = this;
        do  {
            e = new KNOWWE.helper.element(e.parentNode);
        } while( !e._hasClass( clazz ));
        return e ;
    },
    /**
     * Function: _getStyle
     * Returns the style attribute of the current element.
     * 
     * Parameters:
     *     attr - The style attribute of the element one wants to have.
     * 
     * Returns:
     *     The specified style attribute of the element.
     */
    _getStyle : function( attr ){
        return this.style[attr];
    },
    /**
     * Function: _setStyle
     * Sets the style attribute of the current element.
     * 
     * Parameters:
     *     attr - The style attribute of the element one wants to set.
     */
    _setStyle : function( attr , value){
        this.style[attr] = value;
    },
    /**
     * Function: _setText
     * Sets the text of the current DOM element.
     * 
     * Parameters:
     *     text - The text string.
     */
    _setText : function( text ){
        this._setHTML( text );
    },
    /**
     * Function: _setHTML
     * Sets the innerHTML of the current element.
     * 
     * Parameters:
     *     attr - The style attribute of the element one wants to set.
     */    
    _setHTML : function( html ){
        this.innerHTML = html;
    },
    /**
     * Function: _setProperties
     * Allows you to set certain properties of an DOM element. Such properties can be:
     * id, class, href, src, title. Also all attribute that are allowed for the
     * DOM element.
     * 
     * Parameters:
     *     properties - The style attribute of the element one wants to set.
     *     elem - The element the properties should applied to.
     * 
     * Returns:
     *     The enriched element.
     */    
    _setProperties : function( properties , elem){
        if(properties.constructor === Object){
            for(var name in properties){
                var value = (properties[name] || '');
                if(value.constructor === Object){ /* allows objects as properties, like style etc.*/
                    var s = '';
                    for (var i in value){
                        s += i + ':' + value[i] + ';';
                    }
                    value = s;
                }

                name = { 'for': 'htmlFor', 'class': 'className' }[name] || name;
                elem[name] = value;
                
                var node = document.createAttribute( name );
                node.nodeValue = value;
                elem.setAttributeNode(node);
            }
        }
        return elem;    
    },
    /**
     * Function: _remove
     * Removes the current DOM element from the DOM.
     */
    _remove : function (){
        this.parentNode.removeChild( this );
    },
    /**
     * Function: _hide
     * Lets you hide the current DOM element. Therefore it sets the display and
     * visibility of the element to not visible.
     */
    _hide : function(){
        this.style.display = 'none';
        this.style.visibility = 'hidden';
    },
    /**
     * Function: _show
     * Lets you show the current DOM element. Therefore it sets the display and
     * visibility of the element to visible.
     */    
    _show : function(){
        this.style.display = 'inline';
        this.style.visibility = 'visible';
    },
    /**
     * Function: _hasClass
     * Checks if the current element has a given class set.
     * Return true if found, false otherwise.
     * 
     * Parameters:
     *     clazz - The name of the class to be checked.
     * 
     * Returns:
     *     True if found, false otherwise.
     */
    _hasClass : function( clazz ){          
        var pattern = new RegExp('(^|\\s)' + clazz + '(\\s|$)');
        return pattern.test( this.className );  
    },
    /**
     * Function: _addClass
     * Add the given class to the current element
     * 
     * Parameters:
     *     c - The name of the class to be set.
     */
    _addClass : function( c ){
        var clazz = this.className + ' ' + c;
        delete this.className;
        
        this.setAttribute('class', clazz)
        this.setAttribute('className', clazz);      
    },
    /**
     * Function: _attr
     * Returns an attribute of an DOM element. Which can be specified by the
     * parameter.
     * 
     * Parameters:
     *     key - The name of the attribute one want to have.
     * 
     * Returns:
     *     The value of the given attribute. Otherwise null.
     */
    _attr : function( key ){
        try{
            return this.getAttribute( key );
        }catch(e){
            return null;
        }
    },
    /**
     * Function: _inject
     * Adds the current element as a child of a given element.
     * 
     * Parameters:
     *     el - The element the current element should be added.
     */
    _inject : function( el ){
        el.appendChild( this );
    },
    /**
     * Function: _injectTop
     * Adds the current element before the given element.
     * 
     * Parameters:
     *     el - The element the current element should be added.
     */
    _injectTop : function( el ){
        el.insertBefore( this, el.childNodes[0]);
    },
    /**
     * Function: _injectBefore
     * Adds the current element before the parent node of the current
     * DOM element into the DOM.
     * 
     * Parameters:
     *     el - The element the current element should be added.
     */
    _injectBefore : function( el ){
        var parent = el.parentNode;
        parent.insertBefore(this, el);
    },
    /**
     * Function: _injectAfter
     * Adds the current element after the parent node of the current
     * DOM element into the DOM.
     * 
     * Parameters:
     *     el - The element the current element should be added.
     */
    _injectAfter : function( el ){
        var parent = el.parentNode;
        if( parent.lastChild == el ){
            parent.appendChild( this );
        } else {
            parent.insertBefore( this, el.nextSibling );
        }
    }
};


/**
 * Class: KNOWWE.helper.selector
 * Selects and extends DOM elements. Elements arrays returned with this function 
 * will also accept all the HTMLNode methods. The selector can be an id, a HTML 
 * tag or a CSS class and HTML tag in combination. The element can also
 * be selected through an attribute node. Have a look at the examples for 
 * the further possibilities.
 *
 * Examples: (_KS = KNOWWE.helper.selector)
 * - _KS( #id )        returns all elements with id=id
 * - _KS( tag )        returns all elements with tag=tag
 * - _KS( #id tag )    returns all elements with tag=tag within the element with id=id
 * - _KS( tag .class ) returns an array with all elements that agree in tag and class properties
 * - _KS(input[type=submit] returns all submit input elements
 * - _KS(input[type] returns all input elements with the specified type attribute
 * - _KS([type=submit] returns all elements with the given attribute  
 *
 * Parameters:
 *     selector - The selector string used to determine the elements to search for
 *     context - The DOM element that should be used as entry for the element search.
 * 
 * Returns:
 *     The found DOM element(s)
 */
KNOWWE.helper.selector = function(selector, context){
    context = context || document;
    
    if( context.nodeType != 1 && context.nodeType != 9 )  
        return null;
    if( !selector || selector.constructor !== String)
        return null;
    
    var parts = selector.split(' '), t1, t2, t3, i, m;
    while( (m = parts.shift()) ){
        if(t1){
            if(t1.constructor == Array || t1.constructor == Object){
                t3 = new Array();
                var len = t1.length;
                for(i = 0; i < len; i++){
                    context = t1[i];
                    t2 = find( m, context );
                    if( t2.constructor == Array ) {
                        var len2 = t2.length;
                        for(var j = 0; j < len2; j++){
                            t3.push( t2[j] );
                        }
                    }
                    else {
                        t3.push ( t2 );
                    }
                }
            } else {
                t3 = undefined;
                context = t1;
            }
        }
        t1 = t3 || find( m, context );      
    }
    if(!t1){
        //quick fix: wiki page names containing whitespaces
        var el = document.getElementById(selector.replace(/#/, ""));
        if(el) return el
        return null;
    }
    
    var tmp = new Array();
    var k = KNOWWE.helper.element;
    var l = t1.length;
    if(l > 0){
        for(var i = 0; i < l; i++){
           var el = new k( t1[i] );
           tmp.push( el );
        }
        return tmp;
    } else {
        return new k( t1 );
    }    
    /**
     * Searches for the elements in the given context.
     * 
     * Parameters:
     *     selector - The selector for the elements to search
     *     context - The context within to search
     * 
     * Returns:
     *     The found elements.
     */
    function find( selector, context ){
        var i, t;
        var ns = KNOWWE.helper.selector;
        for (i in ns.regex){
            if( ns.regex[i].test( selector )){
                t = ns.filter[i]( context, selector );
                break;
            }
        }
        return t;
    }
};
/**
 * The KNOWWE.helper.selector.regex namespace.
 * Contains the regular expression of the elements that can be found by the
 * ElementSelector. 
 */
KNOWWE.helper.selector.regex = {
    ID : /^#[A-Za-z]+([A-Za-z0-9\s\-\/\_:\.])*$/,
    TAG : /^([A-Za-z0-9])+$/,
    CLASS : /^\.[A-Za-z0-9\-\_]+$/,
    ATTR : /^(\w+)?\[(\w+)=?(\w+)?\]$/
};
/**
 * The KNOWWE.helper.selector.filter namespace.
 * This filters are used to search for the with the selector specified elements.
 * If something is found the elements are returned otherwise an empty array.
 */
KNOWWE.helper.selector.filter = function(){
    return {
        /**
         * Searches for an ID element.
         * 
         * Parameters:
         *     selector - The selector for the elements to search
         *     context - The context within to search
         * 
         * Returns:
         *     The found elements.
         */
        ID : function( context, selector ){
            selector = selector.replace(/#/, '');
            return document.getElementById( selector );
        },
        /**
         * Searches for given tags.
         * 
         * Parameters:
         *     selector - The selector for the elements to search
         *     context - The context within to search
         * 
         * Returns:
         *     The found elements.
         */
        TAG : function( context, selector ){
            var e = context.getElementsByTagName( selector );
            return e;
        },
        /**
         * Searches for a given className.
         * 
         * Parameters:
         *     selector - The selector for the elements to search
         *     context - The context within to search
         * 
         * Returns:
         *     The found elements.
         */
        CLASS : function( context, selector ){          
            if( context === document ){
                var e = document.getElementsByTagName('*'), r = new Array(), t;
                var l = e.length;
                for( var i = 0; i < l; i++){
                    t = this.CLASS( e[i], selector);
                    if( t.length !== 0 ) r.push( t );
                }
                return r;
            }
            selector = selector.replace(/\./, '');
            if(!context.className) return new Array();
            
            if( context.className.indexOf( selector ) !== -1 ){
                return context;
            }
            return new Array();
        },
        /**
         * Searches for a given attribute.
         * 
         * Parameters:
         *     selector - The selector for the elements to search
         *     context - The context within to search
         * 
         * Returns:
         *     The found elements.
         */
        ATTR : function( context, selector ){
            var m, r = new Array(), t, i, l;
            
            m = KNOWWE.helper.selector.regex.ATTR.exec( selector );
            t = context.getElementsByTagName( m[1] || '*' ); /* get all elements with given tag*/
            l = t.length;
            
            for(i = 0; i < l; i++){
                if( m[3] && t[i].getAttribute( m[2] ) === m[3] )
                    r.push( t[i] );
                if( !m[3] && t[i].hasAttribute( m[2] ) )
                    r.push( t[i] );
            }
            return r;
        },
        /**
         * Searches for some special tokens. Shorten the selector strings.
         * Not yet implemented.
         * 
         * Parameters:
         *     selector - The selector for the elements to search
         *     context - The context within to search
         * 
         * Returns:
         *     The found elements.
         */
        SPEZIAL : function( context, selector ){
            //:input :checked :submit :button :selected
            return new Array();
        }
    }
}();



/**
 * Class: KNOWWE.helper.hash
 * Representation of a map.
 * Stores data as key:value pairs.
 * 
 * Parameters:
 *     map - An object of key:value pairs to initialize a map with (optional)
 */
KNOWWE.helper.hash = function ( map ){
    var _data = {};
    if( map && map.constructor === Object){
        for ( var key in map ){
            _data[ key ] = map[ key ];
        }
    }
    
    /**
     * Function: set
     * Adds a key:value pair to the map. If the key already exists in the map
     * it is overwritten and the old value is lost.
     * 
     * Parameters:
     *     key - The key of the value
     *     value - The value to the key
     */
    this.set = function(key, value){
        _data[key] = value;
    }
    /**
     * Function: contains
     * Checks if the a value is already stored in the map. Returns TRUE if it
     * is, otherwise FALSE.
     * 
     * Parameters:
     *     key - The key to be checked.
     * 
     * Returns:
     *     TRUE if found, otherwise FALSE
     */
    this.contains = function( key ){
        if(key || key.constructor !== String) throw new Error('Map.contains awaits a string as argument');
        for(var i in _data){
            if(i === key) return true;
        }           
        return false;
    }
    /**
     * Function: get
     * Gets an value to the given key. If the key is not present in the map
     * NULL is returned.
     * 
     * Parameters:
     *     key - The key to which the value is requested.
     * 
     * Returns:
     *     The value to the key or NULL if not present.
     */
    this.get = function( key ){
        if(key || key.constructor !== String)
            throw new Error('Map.get awaits a string as argument');
        
        return _data[key];
    }
    /**
     * Function: forEach
     * Applies an function to every single element in the map.
     * 
     * Parameters:
     *     fn - The function to apply to the map key:value pairs.
     */
    this.forEach = function( fn ){
        if(!fn || fn.constructor !== Function) throw new Error('Map.forEach awaits a function as argument');
        for(var key in _data){
            fn.call(this, key, _data[key] );
        }
    }
    /**
     * Function: keys
     * Returns the keys of the current map.
     * 
     * Returns:
     *     The keys of the map
     */
    this.keys = function(){
        var a = new Array();
        for(var key in _data)
            a.push(key);
        return a;
    }
    /**
     * Function: size
     * Returns the size of the map
     * 
     * Returns:
     *     The size of the map.
     */
    this.size = function(){
        var i = 0;
        for(var i in _data)
            i++;
        return i;
    }
    /**
     * Function: remove
     * Removes an value from the map
     * 
     * Parameters:
     *     key - The key to be deleted.
     */ 
    this.remove = function( key ){
        if(key || key.constructor != String) throw new Error('Map.remove awaits a string as argument');
        delete _data[key];
    }
    /**
     * Function: toString
     * Returns an string containing all elements of the map aks key:value pair.
     * 
     * Returns:
     *     String representation of the map
     */
    this.toString = function(){
        var s = '';
        for(var key in _data){
            s += key + ':' + _data[key] + '\n';
        }
        return s;
    }   
}

/**
 * Class: KNOWWE.helper.logger
 * The KNOWWE logger.
 * Used to log cross browser messages and for debugging issues. The logger is per default
 * disabled. If you want to use it, please set "inUse" to TRUE. If the logger is enabled
 * you will see a number in the top left corner of the page.
 */
KNOWWE.helper.logger = function(){
    
    var LOGGER_ERROR = 1;
    var LOGGER_INFO  = 2;
    /**
     * Variable: inUse
     * Set to false if you want that the logger does not appear.
     * 
     * Type:
     *     Boolean 
     */
    var inUse = false;
    
    /**
     * Variable: msgCount
     * Stores the numbers of displayed messages
     * 
     * Type:
     *     Integer
     */
    var msgCount = 0;
    
    /**
     * Formattes the current date.
     * 
     * Returns:
     *     The formatted date string
     */
    function getTime(){
        var now = new Date();
        var hours = now.getHours();
        var minutes = now.getMinutes();
        var seconds = now.getSeconds();
        return timeStr = hours + ((minutes < 10) ? ':0' : ':') + minutes
            + ((seconds < 10) ? ':0' : ':') + seconds;
    }
    /**
     * Wraps the to display message into correct HTMLNode and returns it.
     * 
     * Parameters:
     *     msg - The message to display
     *     lvl - The type of the message
     * Returns:
     *     The HTMLNode representation of the message
     */
    function getMessageDisplayHTML( msg, lvl ){
        //var html = '<tr class="@class@"><td>' + getTime() + '</td><td>@msg@</td></tr>';
        var html = '<dt class="@class@">' + getTime() + '</dt><dd class="">@msg@</dd>';
        switch( lvl ){
            case LOGGER_ERROR : 
                html = html.replace(/@class@/, 'log-error-bg');
                break;
            case LOGGER_INFO : 
                html = html.replace(/@class@/, 'log-info-bg');
                break;             
            default :
                break;
        }
        html = html.replace(/@msg@/, formatMessage( msg ));
        return html;
    }
    
    /**
     * Formats the message depending on the object type.
     * At the moment only string, object and array is taken into account.
     * 
     * Parameters:
     *     msg - The message the user wants to log.
     * 
     * Returns:
     *     The formatted message as string.
     */
    function formatMessage( msg ){
        if(!msg) return 'null';
        
        var indent = '', formattedMsg = '';
        var type = typeof msg;

        switch(type){
            case 'string':
            case 'number':
                formattedMsg = msg.toLogger(0);
                break;          
            case 'object':
                if(!msg.length){
                    formattedMsg = objToLogger(msg, 4);
                } else {
                    formattedMsg = msg.toLogger(0);
                }
                break;
            default:
                break;
        }
        return formattedMsg;
    }
    /**
     * Formats an object element.
     * 
     * Parameters:
     *     object - The object/array to be formatted.
     *     space - A string used to indent.
     * 
     * Returns:
     *     The formatted string.
     */    
    function objToLogger (obj, indent){
        var s = '{<br />', oldIndent = indent;
        for( var i in obj ){
            if( typeof obj[i] == 'function') continue;
            var space = KNOWWE.helper.logger.space( indent );
            if(typeof obj[i] == 'object'){
                s += space + i + ':';
                indent += oldIndent;
                s += objToLogger(obj[i], indent) + '<br />';
                indent -= oldIndent;
            }
            else {
                s += space + i + ':' + obj[i].toLogger(0) + '<br />';
            }
        }
        s += KNOWWE.helper.logger.space(indent-oldIndent) + '}';
        return s;
    }
    
    /**
     * Toggles the logger message window.
     */
    function toogle( ){
        var element = document.getElementById('KNOWWE-logger-display').getElementsByTagName('dl')[0].style;
        var show = element['display'];
        if( show == "" ) show = 'inline';
        element.display = (show === 'inline' ) ? 'none' : 'inline';
        document.getElementById('KNOWWE-logger-clear').style.display = (show === 'inline' ) ? 'none' : 'inline';
    }
    /**
     * Function: clear
     * Clears the logger messages. And resets the message counter to zero.
     */
    function clear(){
        msgCount = 0;
        document.getElementById('KNOWWE-logger-count').innerHTML = msgCount;
        document.getElementById('KNOWWE-logger-display').getElementsByTagName('dl')[0].innerHTML = "";
        toogle();
    }
    /**
     * Logs the message with the given level to the message panel. Logs only if the
     * logger should be used. This is indicated by the <code>inUse</code> property.
     * 
     * Parameters:
     *     msg - The to log message.
     *     lvl - The level of the message.
     */
    function log ( msg, lvl ){
        if(!inUse) return;
        msgCount = (msgCount * 1 )+ 1;
        document.getElementById('KNOWWE-logger-count').innerHTML = msgCount;
    
        var oldMsg = document.getElementById('KNOWWE-logger-display').getElementsByTagName('dl')[0];
        oldMsg.innerHTML = getMessageDisplayHTML( msg, lvl ) + oldMsg.innerHTML;
    }
    
    /**
     * Initializes the Logger. Only if the logger should be used.
     * This is indicated by the <code>inUse</code> property.
     */
    function init(){
       if(!inUse) return;
        var domEl = document.createElement('div');
        var domAtt = document.createAttribute('id');
        domAtt.nodeValue = 'KNOWWE-logger-main';
        domEl.setAttributeNode( domAtt );
        domEl.innerHTML = '<div id="KNOWWE-logger-header" class="pointer">' 
            + '<span id="KNOWWE-logger-count">' + msgCount + '</span>'
            + '<span id="KNOWWE-logger-clear" class="right" style="display:none">[clear]</span></div>'
            + '<div id="KNOWWE-logger-display"><dl></dl></div>';

        document.getElementsByTagName("body")[0].appendChild( domEl );
        
        var loggerCount = document.getElementById('KNOWWE-logger-count'); 
        loggerCount.innerHTML = msgCount;
        KNOWWE.helper.event.add('click', loggerCount, toogle );
        KNOWWE.helper.event.add('click', _KS('#KNOWWE-logger-clear'), clear );
        toogle();
    }
    
    return {
        /**
         * Function: setup
         * Initializes the logger if it has not yet.
         */
        setup : function(){
            if(document.getElementById('KNOWWE-logger-main')) return;
            init();
        },      
        /**
         * Function: error
         * Logs the given message to the overlay HTMLNode DIV container as a debug
         * message.
         * 
         * Parameters:
         *     msg - The message to display
         */        
        error : function( msg ){
            if(!document.getElementById('KNOWWE-logger-main')){
                init();
            }
            log( msg, LOGGER_ERROR );
        },
        /**
         * Function: info
         * Logs the given message to the overlay HTMLNode DIV container as a debug
         * message.
         * 
         * Parameters:
         *     msg - The message to display
         */        
        info : function( msg ){
            if(!document.getElementById('KNOWWE-logger-main')){
                init();
            }
            log( msg, LOGGER_INFO );
        },
        /**
         * Function: space
         * Returns a string with the given length of space characters.
         * 
         * Parameters:
         *     len - The length of the space string.
         * Returns:
         *     A string containing len space characters.
         */
        space : function( len ){
            var s = '';
            for(var i = 0; i < len; i++){
                s += '&nbsp;';
            }
            return s;
        }
    }
}();

/**
 * Class: KNOWWE.helper.observer
 * The observer namespace.
 * 
 * Returns:
 *     An observer object.
 */
KNOWWE.helper.observer = function(){
	/**
	 * Stores the functions that should notified.
	 */
	var observations = [];
	return {
	    /**
	     * Function: subscribe
	     * 
	     * Parameters:
	     *     fn - The function that should be registered. 
	     */
	    subscribe : function( fn ){
	        try{
	            if( typeof fn !== "function" ) throw 'subscribe argument is not a function: ';
	            observations.push( fn );
	        } catch( err ){
	            alert( err + fn); //not the best error handling
	        }
	    },
	    /**
	     * Function: unsubscribe
	     * 
	     * Parameters:
	     *     fn - The function that should be unregistered.
	     */
	    unsubscribe : function( fn ) {
	        var l = observations.length;
	        for( var i = 0; i < l; i++){
	            if( observations[i] === fn ){
	                observations.splice(i,1);
	            }
	        }
	    },
	    /**
	     * Function: notify
	     * Notifies all the registered observers and executes their actions.
	     * 
	     * Parameters:
	     *     o - The current scope.
	     */
	    notify : function( o ) {
	        var scope = o || window;
	        var l = observations.length;
	        for( var i = 0; i < l; i++){
	            observations[i].call( o );
	        }
	    }	
	}
}();

/**
 * Class: KNOWWE.helper.overlay
 * The overlay namespace.
 * 
 * Parameters:
 *     options - An options object with parameters for the overlay element.
 * 
 * Returns:
 *     The overlay element.
 */
KNOWWE.helper.overlay = function( options ){
    var o = {}; /* contains the overlay HTMLElement*/

    /**
     * Variable: oDefault
     * Contains the default options of the dialog.
     * 
     * Type:
     *     Object
     */
    var oDefault = {
       id : 'o-lay',
       mainCSS : 'o-lay-body',
       title : '&nbsp;',
       css : {
           display : 'none',
           position : 'absolute',
           zIndex : 1000,
           minWidth:'300px'
       },
       cursor : {
           top : 0,
           left : 0
       },
       url : null,
       content : null,
       fn : null
    };
    oDefault = KNOWWE.helper.enrich( options, oDefault );

    init();
    return o;
    
    /**
     * Function: init
     * Initializes the overlay. Therefore gets the data that should be displayed
     * and wraps it into an HTMLElement.
     */
    function init(){
        o = new KNOWWE.helper.element('div', {
            id : oDefault.id
        });
        o._css( oDefault.css );
        var c = (oDefault.content) ? oDefault.content : '';
        o._setHTML( '<div id="o-lay-wrapper"><div id="o-lay-top"><div id="o-lay-title">'
                + oDefault.title + '<span class="right pointer" id="o-lay-close">x</span></div></div>' 
                + '<div id="' + oDefault.mainCSS +'">' + c + '</div></div>');
        document.getElementsByTagName('body')[0].appendChild(o);

        if( oDefault.url ){
            var options = {
                url : oDefault.url,
                response : {
                    action: 'insert',
                    ids : [ oDefault.mainCSS ],
                    fn : function(){
                        var olay = KNOWWE.helper.selector('#' + oDefault.id);
                        olay._css({top : oDefault.cursor.top + 'px', left: oDefault.cursor.left + 'px'});
                        olay._show();
                        oDefault.fn.call();
                    }
                }
            };
            new KNOWWE.helper.ajax( options ).send();
        } else if(oDefault.content) {
            var olay = KNOWWE.helper.selector('#' + oDefault.id);
            olay._css({top : oDefault.cursor.top + 'px', left: oDefault.cursor.left + 'px'});
            olay._show();
            if(oDefault.fn)
                oDefault.fn.call();
        }
    }
}


/**
 * Class: KNOWWE.helper.window
 * The KNOWWE window object.
 * Used to handle pop-up windows.
 */
KNOWWE.helper.window = function(){
    var instance;
    return {
        /**
         * Function: open
         * Shows a pop-up window with the given parameter.
         * 
         * Parameters:
         *     options - An object containing the configuration of the window
         */
        open : function( options ){
            var oDefault = {
                resizable : 'yes',
                toolbar : 'no',
                menubar : 'no',
                scrollbars : 'yes',
                location : 'no',
                status : 'yes',
                dependent : 'yes',
                url : 'KnowWE.jsp',
                height : 420,
                width : 520,
                screenWidth : (window.screen.width/2) - (210 + 10),
                screenHeight : (window.screen.width/2) - (210 + 10),
                left : 0, 
                top : 0,
                screenX : 0,
                screenY : 0
            }
            oDefault = KNOWWE.helper.enrich( options, oDefault );
            instance = window.open(oDefault.url, 'KnowWEPopup', KNOWWE.core.util.getWindowParams( oDefault ));
            instance.focus;
        },
        /**
         * Function: getWindow
         * Returns the current window.
         * 
         * Returns:
         *      The instance of the pop-up
         */
        getWindow : function(){
            return instance;
        }
    }
}();

if( !String.getBytes ){
     /**
     * Class: String.getBytes
     * Encodes this String into a sequence of bytes,
     * storing the result into a new string encoded as a component of a URI.
     * 
     * Returns:
     *     The encoded String.
     */
    String.prototype.getBytes = function(){
            return encodeURIComponent(this).replace(/%../g, 'x').length;
    };
}
if( !String.startsWith ){
    /**
     * Class: String.startsWith
     * Determines whether the beginning of an instance of String matches a specified string.
     * 
     * Parameters:
     *     str - The prefix used for checking
     */
    String.prototype.startsWith = function( str ) {
        return (this.match("^"+str) == str);
    };
}
if( !String.endsWith ){ 
    /**
     * Class: String.endsWith
     * Determines whether the end of an instance of String matches a specified string..
     * 
     * Parameters:
     *     str - The prefix used for checking
     */
    String.prototype.endsWith = function(str){
        return (this.match(str+"$") == str);
    };
}

if (!Array.prototype.each)
{
    /**
     * Class: String.each
     * Iterates over an array and executes the given function for each element of the 
     * array. Used to simply apply a function to all elements of an array.
     * 
     * This prototype is provided by the Mozilla foundation and
     * is distributed under the MIT license.
     * http://www.ibiblio.org/pub/Linux/LICENSES/mit.license
     * 
     * Parameters:
     *     fun - The called function.
     */
    Array.prototype.each = function(fun /*, thisp*/)
    {
      var len = this.length >>> 0;
      if (typeof fun != "function")
        throw new TypeError();
    
      var thisp = arguments[1];
      for (var i = 0; i < len; i++)
      {
        if (i in this)
          fun.call(thisp, this[i], i, this);
      }
    };
}


/**
 * Function: Array.toLogger
 * Converts an array to a string.
 * 
 * Parameters:
 *     indent - The indent of the line
 * 
 * Returns:
 *     The formatted string
 */
Array.prototype.toLogger = function(indent){
    var space = KNOWWE.helper.logger.space(indent), s = '[';
    var len = this.length >>> 0;

    for (var i = 0; i < len; i++){
      if (i in this){
          if(typeof this[i] == 'function') continue;
          if(typeof this[i] == 'object'){
              if(this[i].tagName){
                  var tag = this[i].tagName.toLowerCase();
                  var attr = KNOWWE.helper.formatAttributes(this[i].attributes);

                  s += tag + ':' + attr;                  
              } else {
                  s += this[i];  
              }
          } else {
              s += ' ' + this[i].toLogger(indent);
          }
      }
      if(i + 1 != len) s += ',';
    }
    s += ' ]';
    return s;
}
/**
 * Function: String.toLogger
 * Converts an array to a string.
 * 
 * Parameters:
 *     indent - The indent of the line
 * 
 * Returns:
 *     The formatted string
 */
String.prototype.toLogger = function(indent){
    var space = KNOWWE.helper.logger.space(indent);
    return space + '"' + this + '"';
}
