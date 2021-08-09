var bratLocation = 'assets/brat';

// Color names used
var entityColor = '#AED6F1';

head.js(
    // External libraries
    bratLocation + '/client/lib/jquery.min.js',
    bratLocation + '/client/lib/jquery.svg.min.js',
    bratLocation + '/client/lib/jquery.svgdom.min.js',

    // brat helper modules
    bratLocation + '/client/src/configuration.js',
    bratLocation + '/client/src/util.js',
    bratLocation + '/client/src/annotation_log.js',
    bratLocation + '/client/lib/webfont.js',

    // brat modules
    bratLocation + '/client/src/dispatcher.js',
    bratLocation + '/client/src/url_monitor.js',
    bratLocation + '/client/src/visualizer.js'
);

var webFontURLs = [
    bratLocation + '/static/fonts/Astloch-Bold.ttf',
    bratLocation + '/static/fonts/PT_Sans-Caption-Web-Regular.ttf',
    bratLocation + '/static/fonts/Liberation_Sans-Regular.ttf'
];

var collData = {
    entity_types: [ {
        "type"   : "Entity",
        "labels" : ["Entity"],
        // Blue is a nice colour for a person?
        "bgColor": entityColor,
        // Use a slightly darker version of the bgColor for the border
        "borderColor": "darken"
        },
    ]
    //    event_types: [
    //      {
    //          "type": "Alpha",
    //          "labels": ["Alpha"],
    //          "bgColor": "lightgreen",
    //          "borderColor": "darken",
    //          "arcs": []
    //       },
    //    ]

};

// docData is initially empty.
var docData = {};

head.ready(function() {

    var code = $(".codemirror-textarea")[0];
    var editor = CodeMirror.fromTextArea(code, {
        lineNumbers : true,
        comment: true,
        matchBrackets: true,
        autoCloseBrackets: true,
        extraKeys: {
                    "Tab": function(cm){
                        cm.replaceSelection("   " , "end");
                    },
                    "Ctrl-/": function(cm){
                        editor.toggleComment({
                            indent: true
                        });
                    },
                    "Cmd-/": function(cm){
                        editor.toggleComment({
                            indent: true
                        });
                    }
                   }
    });

    var syntaxLiveDispatcher = Util.embed('syntax',
        $.extend({'collection': null}, collData),
        $.extend({}, docData),
        webFontURLs
    );
    var eidosMentionsLiveDispatcher = Util.embed('mentions',
        $.extend({'collection': null}, collData),
        $.extend({}, docData),
        webFontURLs
    );

    $('form').submit(function (event) {

        // stop the form from submitting the normal way and refreshing the page
        event.preventDefault();

        // collect form data
        var formData = {
            'sent': $('textarea[name=text]').val(),
            'rules': $('textarea[name=rules]').val()
        }

        if (!formData.sent.trim()) {
            alert("Please write some text.");
            return;
        }

        if (!formData.sent.trim()) {
            alert("Please write one or more rules.");
            return;
        }

        // show spinner
        document.getElementById("overlay").style.display = "block";
        console.log(formData);
        // process the form
        $.ajax({
            type: 'GET',
            url: 'processTextDynamic',
            data: formData,
            dataType: 'json',
            encode: true
        })
        .fail(function () {
            // hide spinner
            document.getElementById("overlay").style.display = "none";
            alert("error");
        })
        .done(function (data) {
            console.log(data);
            syntaxLiveDispatcher.post('requestRenderData', [$.extend({}, data.syntax)]);
            eidosMentionsLiveDispatcher.post('requestRenderData', [$.extend({}, data.mentions)]);
            document.getElementById("groundedAdj").innerHTML = data.mentionDetails;
            document.getElementById("parse").innerHTML = data.parse;
            // hide spinner
            document.getElementById("overlay").style.display = "none";
        });

    });
});