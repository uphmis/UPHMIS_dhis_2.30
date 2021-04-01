
jQuery(document).ready(function()
{
    validation2('saveDeleteLockExceptionForm', function( form )
    {
    	validateLockExceptionForm(form);
    },
    {
        'beforeValidateHandler' : function() {
            $("#selectedPeriods option").each(function() { $(this).attr("selected", "true"); });
            $("#selectedDataSets option").each(function() { $(this).attr("selected", "true"); });
        },
        //'rules' : getValidationRules("dataLocking")
    });
});

// ------------------------------------------------------------------------------
// Get Periods and DataSets correspond to Selected Period Type
// ------------------------------------------------------------------------------

function getPeriodsAndDataSets()
{
	$('#selectedPeriods >option').remove();
	$('#selectedDataSets >option').remove();
	
	$('#availablePeriods >option').remove();
	$('#availableDataSets >option').remove();
	
	
	document.getElementById( "lockException" ).disabled = true;
	document.getElementById( "delete" ).disabled = true;
	document.getElementById( "availablePeriods" ).disabled = true;
	document.getElementById( "availableDataSets" ).disabled = true;
	
	var periodTypeId = $( '#periodTypeId' ).val();
	
	/*
	var periodTypeList = byId("periodTypeId");
	var periodTypeId = periodTypeList.options[periodTypeList.selectedIndex].value;
	*/
	
	if ( periodTypeId == "-1" )
	{
		setHeaderDelayMessage( i18n_period_type_not_selected );
		return false;
	}
	
	else
	{
		$.post("getPeriodsAndDataSetList.action",
				{
					periodTypeName:periodTypeId
				},
				function(data)
				{
					populatePeriodsAndDataSetList( data );
				},'xml');
	}
	
}


function populatePeriodsAndDataSetList( data )
{
	
	enable("lockException");
	enable("delete");
	enable("availablePeriods");
	enable("availableDataSets");
	
	var availablePeriods = document.getElementById("availablePeriods");
	clearList( availablePeriods );
	
	var periodList = data.getElementsByTagName("period");
		
	for ( var i = 0; i <  periodList.length; i++ )
	{
		var id = periodList[ i ].getElementsByTagName("id")[0].firstChild.nodeValue;
		var name = periodList[ i ].getElementsByTagName("name")[0].firstChild.nodeValue;
		
		var option = document.createElement("option");
		option.value = id;
		option.text = name;
		option.title = name;
		availablePeriods.add(option, null);
	} 

	var availableDataSets = document.getElementById("availableDataSets");
	clearList( availableDataSets );
	
	var dataSetList = data.getElementsByTagName("dataset");
	
	for ( var i = 0; i < dataSetList.length; i++ )
	{
		var id = dataSetList[ i ].getElementsByTagName("id")[0].firstChild.nodeValue;
		var name = dataSetList[ i ].getElementsByTagName("name")[0].firstChild.nodeValue;
		
		var option = document.createElement("option");
		option.value = id;
		option.text = name;
		option.title = name;
		availableDataSets.add(option, null);
	}
}

/*
function getPeriods()
{
	var periodTypeList = byId("periodTypeId");
	var periodTypeId = periodTypeList.options[periodTypeList.selectedIndex].value;

	if (periodTypeId != null) {
		var url = "getPeriodsForLockException.action?periodTypeName=" + periodTypeId;
		$.ajax( {
			url :url,
			cache :false,
			success : function(response) {
				dom = parseXML(response);
				$('#availablePeriods >option').remove();
				$(dom).find('period').each(
						function() {
							$('#availablePeriods').append(
									"<option value="
											+ $(this).find('id').text() + ">"
											+ $(this).find('name').text()
											+ "</option>");
						});
			}

		});
	}
	
	enable("lockException");
	enable("delete");
	enable("availablePeriods");
	
	getDataSets();

}

function parseXML(xml) {

	if (window.ActiveXObject && window.GetObject) {
		var dom = new ActiveXObject('Microsoft.XMLDOM');
		dom.loadXML(xml);
		return dom;
	}
	if (window.DOMParser)
		return new DOMParser().parseFromString(xml, 'text/xml');
	
	throw new Error('No XML parser available');
}

function getDataSets() {

	var periodTypeList = byId("periodTypeId");
	var periodType = periodTypeList.options[periodTypeList.selectedIndex].value;

	if (periodType != null) {
		var url = "getDataSetsForPeriodType.action?periodType=" + periodType;
		$.ajax( {
			url :url,
			cache :false,
			success : function(response) {
			$('#availableDataSets >option').remove();
			$(response).find('dataSet').each(
					function() {
						$('#availableDataSets').append(
								"<option value=" + $(this).find('id').text()
										+ ">" + $(this).find('name').text()
										+ "</option>");
					});
			enable("availableDataSets");
			$('#selectedPeriods >option').remove();
			$('#selectedDataSets >option').remove();
		}
		});
	}
}
*/


function setClickedButtonElementValue( isLock )
{
	//document.ChartGenerationForm.selectedButton.value = selButton;
	
	document.getElementById("selectBetweenLockException").value = isLock;
	
	//setFieldValue("selectBetweenLockException", isLock);
	//alert( isLock )
	//alert( selectBetweenLockException );
}

function validateLockExceptionForm( form )
{
	var url = "validateLockException.action?";
		url += getParamString( "selectedPeriods", "selectedPeriods" );
		url += "&" + getParamString( "selectedDataSets", "selectedDataSets" );

	$.postJSON( url, {}, function( json )
	{
		if ( json.response == "input" )
		{
			setHeaderDelayMessage( json.message );
		}
		else if ( json.response == "success" )
		{
			selectAllById( "selectedPeriods" );
			selectAllById( "selectedDataSets" ); 
			form.submit();
		}
	});
}

