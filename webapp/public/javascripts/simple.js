
$(document).ready(function () {

    var form = $("#svoForm");
    $('form').submit(function (event) {

        // stop the form from submitting the normal way and refreshing the page
        event.preventDefault();

        // collect form data
        var ruleName = $('#rulename').val();

        // Subject
        var subjectData = Object();
        // Label or default placeholder
        subjectData.label = $('#sLabel').val();
        if (!subjectData.label.trim()) {
            subjectData.label = document.getElementById("sLabel").placeholder;
        };
        subjectData.words = $('#sWords').val();
        subjectData.argType = form.find("input[name=subj_radio]:checked").val();

        // Verb
        var verbData = Object();
        // Label or default placeholder
        verbData.label = $('#vLabel').val();
        if (!verbData.label.trim()) {
            verbData.label = document.getElementById("vLabel").placeholder;
        };
        verbData.words = $('#vWords').val();
        verbData.argType = form.find("input[name=verb_radio]:checked").val();

        // Object
        var objectData = Object();
        // Label or default placeholder
        objectData.label = $('#oLabel').val();
        if (!objectData.label.trim()) {
            objectData.label = document.getElementById("oLabel").placeholder;
        };
        objectData.words = $('#oWords').val();
        objectData.argType = form.find("input[name=obj_radio]:checked").val();

        var data = Object();
        data.ruleName = $('#rulename').val();
        data.subj = subjectData;
        data.verb = verbData;
        data.obj = objectData;

        var formData = {
            "data": JSON.stringify(data)
        };

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
            url: 'buildRules',
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
                var args = ruleData[0]
                var nArgs = args.length
                var ruleRows = ruleData[1]
                var ruleName = ruleData[2]
                $('#tablecaption').text(ruleName);
                $('#results').DataTable({
                    colReorder: true,
                    destroy: true,
                    data: ruleRows,
                    // dynamically make the column headers to match the argument names
                    columnDefs: mkColumnDefs(args),
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

// --------------------------------------------------------------------------------------------------------
//                                      TABLE FORMATTING METHODS
// --------------------------------------------------------------------------------------------------------


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

// processing the results into the format we need
function ruleDisplay (r) {
    var rule = r.rule;
    var args = r.args[0];
    var data = [];
    var results = r.results;
    for (let i = 0; i < results.length; i ++) {
        cells = {};
        var curr = results[i];
        for (let j = 0; j < args.length; j ++) {
            var jString = j.toString();
            cells[jString] = curr.result[0][j];
        }
        cells["count"] = curr.count;
        cells["evidence"] = curr.evidence;
        data.push(cells);
    }

    return [args, data, rule];

}

// formatting the data point into a row in the table
function mkColumns(nArgs) {
    var columns = [];
    columns.push({
         "className":      'details-control', // the evidence button
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

// Making the table headers (dynamically)
function mkColumnDefs(args) {
    var columnDefs = [];

    // The expand/collapse column
    var firstCol = {};
    firstCol.targets = 0;
    firstCol.className ="dt-center";
    firstCol.width = "5px";
    columnDefs.push(firstCol);

    // Argument columns
    for (let i = 0; i < args.length; i++) {
       var cell = {};
       cell.targets = i+1;
       cell.title = args[i]
       columnDefs.push(cell);
    }

    // Count column
    var cell = {};
    cell.targets = args.length + 1;
    cell.title = "count";
    cell.className ="dt-center";
    cell.width = "30px";
    columnDefs.push(cell);

    return columnDefs;
}
