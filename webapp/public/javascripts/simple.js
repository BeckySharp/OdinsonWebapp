
function inPage(elem) {
    var elemObj = document.getElementById(elem);
    if(typeof elemObj === 'undefined') {
            return false;
        } else if(elemObj === null){
            return false;
        } else {
            return true
        }
}

$(window).on("load", function () {



    // -----------------------------------------------
    //           Form for querying the index
    // -----------------------------------------------
    var form = $("#svoForm");
    form.submit(function (event) {

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
        data.mods = [];
        console.log("nMods:", nMods);

        for (let i = 0; i < nMods; i ++) {
            var modData = Object();
            // Label or default placeholder
            var modId = 'mod' + i;
            var modLabel = modId + 'Label';

            if (inPage(modLabel)) {
                modData.label = $('#' + modLabel).val();
                if (!modData.label.trim()) {
                    modData.label = document.getElementById(modLabel).placeholder;
                };
                var modWords = modId + 'Words';
                modData.words = $('#' + modWords).val();
                var modRadio = modId + "Radio";
                modData.argType = form.find("input[name=" + modRadio + "]:checked").val();
                data.mods.push(modData);
            }



        }


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
            // Display if there are no results
            if (data.length == 0) {
                $('#tablecaption').text("Rule returned no results.");
            }
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
                    columns: mkColumns(nArgs)
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

    // -----------------------------------------------
    //           Form for adding modification
    // -----------------------------------------------
    var nMods = 0;
    var modForm = $("#modifierForm");
    var selectedMods = [];
    modForm.submit(function (event) {
            // Enable the add button now that there has been a search
            $('#addModBtn').attr("disabled", false);

            // stop the form from submitting the normal way and refreshing the page
            event.preventDefault();

            // Get search term
            var searchTerm = $('#searchTerm').val();
            var modFormData = {
                'query': searchTerm
            }
            if (!modFormData.query.trim()) {
                alert("Please write something.");
                return;
            }

            // if there's already a table, clear it
            if ($.fn.DataTable.isDataTable('#modTable')) {
                $('#modTable').DataTable().clear().destroy();
            }
            // Clear the search box?
            // $('#searchTerm').val('')

            // show spinner
//            document.getElementById("overlay").style.display = "block";

            // process the modifier form
            $.ajax({
                type: 'GET',
                url: 'getSimilarMods',
                data: modFormData,
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
                var table = $('#modTable').DataTable({
                    searching: false,
                    lengthMenu: [ 5, 10, 25],
//                    pageLength: 5,
//                    lengthChange: false,
                    select: {
                             'style': 'multi'
                            },
                    order: [[2, 'desc']],
                    data: data,
                    columnDefs: [
                         {
                            'targets': 0,
                            'checkboxes': {
                               'selectRow': true
                            }
                         }
                    ],
                    columns: [
                        { title: "select" },
                        { title: "result" },
                        { title: "score" },
                    ]
                });
                // hide spinner
                document.getElementById("overlay").style.display = "none";

                // Handle form submission event
                // the "unbind" is prob not officially correct, but the function/click listener was
                // getting bound each time, so multiple submissions were happening.
               $('#addModBtn').unbind('click').on('click', function(e){
                  var form = this;
                  var selectedMods = [];

                  var rows_selected = table.column(0).checkboxes.selected();

                  // Iterate over all selected checkboxes to gather the selections
                  $.each(rows_selected, function(index, rowId){
                     // Create a hidden element
                     selectedMods.push(
                         data[rowId][1]
                     );
                  });
                  // Create the row and add it
                  var modId = "mod" + nMods;
                  var argInitName = "custom_" + nMods;
                  addSVORow("svoTable", modId, "custom", argInitName, selectedMods.join());

                  // Disable the add button again until there has been another search
                  $('#addModBtn').attr("disabled", true);
                  // Increment the number of mods so each has a unique name/id
                  nMods += 1;

               });
            });


    });

// --------------------------------------------------------------------------------------------------------
//                               RULE BUILDING TABLE FORMATTING METHODS
// --------------------------------------------------------------------------------------------------------

    // Adds row to the SVO/rule workspace main table
    function addSVORow(tableID, rowPrefix, role, label, constraints) {
      // Get a reference to the table
      let tableRef = document.getElementById(tableID).getElementsByTagName('tbody')[0];

      // Insert a row at the end of the table
       var newRow = createSVORowElement(rowPrefix, role, label, constraints);
       tableRef.appendChild(newRow);
    }

    /** Method for creating new rows to go into the SVO main table.  Each row corresponds
     *  to a modifier, as specified in the top right container.
     */
    function createSVORowElement(rowPrefix, role, label, constraints) {
        // first 3 columns (role, label, and constraints)
        var col1Html = "<td> " + role + "</td>";
        var col2Html = '<td> <input type="text" id="' + rowPrefix + 'Label" placeholder="' + label + '"> </td>';
        var col3Html = '<td> <input type="text" id="' + rowPrefix + 'Words" value="' + constraints + '"> </td>';
        // radio buttons
        var col4Html = '<td> <input type="radio" name="' + rowPrefix + 'Radio" value="required" checked> </td>';
        var col5Html = '<td> <input type="radio" name="' + rowPrefix + 'Radio" value="optional">  </td>';
        // hidden unless hovered delete icon
        var col6Html = '<td class="tight-left"> <button type="button" class="delete"></button> </td>';

        var rowFragment = document.createElement('tr');
        rowFragment.classList.add("pad")
        rowFragment.innerHTML = col1Html + col2Html + col3Html + col4Html + col5Html + col6Html;

        return rowFragment;
    }

    function createRadioElement(name, value, checked) {
        var radioHtml = '<input type="radio" name="' + name + '"';
        radioHtml += ' value="' + value +'"';
        if ( checked ) {
            radioHtml += ' checked';
        }
        radioHtml += '/>';

        var radioFragment = document.createElement('div');
        radioFragment.innerHTML = radioHtml;

        return radioFragment.firstChild;
    }

    // Delete a row from the table, with a confirmation check
    $('table').on('click', 'button[type="button"]', function(e){
        Swal.fire({
          title: 'Remove argument',
          text: "Are you sure you want to delete this argument?",
          icon: 'question',
          showCancelButton: true,
          confirmButtonColor: '#255497',
          cancelButtonColor: '#d33',
          confirmButtonText: 'Delete'
        }).then((result) => {
          if (result.value) {
            $(this).closest('tr').remove()
          }
        })
    });




});



// --------------------------------------------------------------------------------------------------------
//                                   RESULTS TABLE FORMATTING METHODS
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
