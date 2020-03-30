/* Formatting function for row details - here the evidence */
function format ( d ) {
// `d` is the original data object for the row
    var t = '<table cellpadding="5" cellspacing="0" border="0" style="padding-left:50px;">'
    t = t + '<tr class="grey">' +
         '<td> DocID </td>' +
         '<td> Sentence Text </td>' +
        '</tr>';
    for (let i = 0; i < d.evidence.length; i++) {
        t = t + '<tr>' +
                 '<td>' + d.evidence[i].id + '</td>' +
                 '<td>' + d.evidence[i].sentence + '</td>' +
                '</tr>';
    }
    return t + '</table>';
}

function ruleDisplay (r) {
    var rule = r.rule
    var arguments = r.arguments[0];
    var data = []
    var results = r.results
    for (let i = 0; i < results.length; i ++) {
        cells = {}
        var curr = results[i]
        for (let j = 0; j < arguments.length; j ++) {
            var jString = j.toString()
            cells[jString] = curr.result[0][j]
        }
        cells["count"] = curr.count
        cells["evidence"] = curr.evidence
        data.push(cells)
    }

    return [arguments, data, rule];

}

function mkColumns(nArgs) {
    var columns = [];
    columns.push({
         "className":      'details-control',
         "orderable":      false,
         "data":           null,
         "defaultContent": ''
     });
    for (let i = 0; i < nArgs; i ++) {
       var cell = {};
       cell.data = i.toString();
       columns.push(cell);
    }
    columns.push({data: "count"})
    return columns;
}

function mkColumnDefs(arguments) {
    var columnDefs = [];

    // The expand/collapse column
    var firstCol = {};
    firstCol.targets = 0;
    firstCol.className ="dt-center";
    firstCol.width = "5px";
    columnDefs.push(firstCol);

    // Argument columns
    for (let i = 0; i < arguments.length; i++) {
       var cell = {};
       cell.targets = i+1;
       cell.title = arguments[i]
       columnDefs.push(cell);
    }

    // Count column
    var cell = {};
    cell.targets = arguments.length + 1;
    cell.title = "count";
    cell.className ="dt-center";
    cell.width = "30px";
    columnDefs.push(cell);

    return columnDefs;
}



$(document).ready(function () {

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
//                        editor.execCommand('toggleComment');
                    },
                    "Cmd-/": function(cm){
                        editor.toggleComment({
                            indent: true
                        });
//                        editor.execCommand('toggleComment');
                    }
                   }
    });

    $('form').submit(function (event) {

        // stop the form from submitting the normal way and refreshing the page
        event.preventDefault();

        // collect form data
        var rules = $('#rules').val();
        var formData = {
            'rules': rules
        }

        if (!formData.rules.trim()) {
            alert("Please write something.");
            return;
        }

        if ($.fn.DataTable.isDataTable('#results')) {
            $('#results').DataTable().clear().destroy();
            // to handle the fact that the thead remnants stay around:
            // ref: https://datatables.net/forums/discussion/20524/destroy-issue
            $('#results').empty();
            $('#results').html('<caption id="tablecaption"></caption>');
        }


        // show spinner
        document.getElementById("overlay").style.display = "block";

        // process the form
        $.ajax({
            type: 'GET',
            url: 'customRules',
            data: formData,
            dataType: 'json',
            encode: true
        })
        .fail(function (jqXHR, textStatus) {
            // hide spinner
            document.getElementById("overlay").style.display = "none";
            console.log(jqXHR);
            console.log(textStatus);
            alert("request failed: " + textStatus);
        })
        .done(function (data) {
            for (let i = 0; i < data.length; i++) {
                var ruleData = ruleDisplay(data[i])
                var arguments = ruleData[0]
                var nArgs = arguments.length
                var ruleRows = ruleData[1]
                var ruleName = ruleData[2]
                $('#tablecaption').text(ruleName);
                $('#results').DataTable({
                    colReorder: true,
                    destroy: true,
                    data: ruleRows,
                    // dynamically make the column headers to match the argument names
                    columnDefs: mkColumnDefs(arguments),
                    // dynamically make the columns with data for this rule
                    columns: mkColumns(nArgs),
                    });
            }

            // hide spinner
            document.getElementById("overlay").style.display = "none";
        });

    });

    // Add event listener for opening and closing (evidence) details
    $(document).on('click', 'td.details-control', function () {
        var tr = $(this).closest('tr');
        var row = $('#results').DataTable().row( tr );

        if ( row.child.isShown() ) {
            // This row is already open - close it
            row.child.hide();
            tr.removeClass('shown');
        }
        else {
            // Open this row
            row.child( format(row.data()) ).show();
            tr.addClass('shown');
        }
    } );

});
