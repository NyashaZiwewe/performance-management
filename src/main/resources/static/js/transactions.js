
function processPaymentTypeChangeUploads(pType) {
    //get all payment types div
    var chequeDiv = dojo.byId('cheque_inputs');
    var posDiv = dojo.byId('pos_inputs');
    var rtgsDiv = dojo.byId('rtgs_inputs');
    var ecocashDiv = dojo.byId('ecocash_inputs');
    var voucherDiv = dojo.byId('voucher_inputs');
    //hide all
    chequeDiv.className = 'hideControl';
    posDiv.className = 'hideControl';
    rtgsDiv.className = 'hideControl';
    ecocashDiv.className = 'hideControl';
    voucherDiv.className = 'hideControl';

    //show appropriate
    //Default Value Set here
    var temp = pType;
    //console.log(">> temp : "+temp);
    var mySelect = dojo.byId('paymentType');
    //console.log(">> mySelect : "+mySelect);
    for(var i, j = 0; i = mySelect.options[j]; j++) {
        //console.log(">> i : "+i+", j : "+j);
        //console.log(">> option: "+i.value);
        if(i.value === temp) {
            //console.log(">> values are equal, about to default option to "+temp);
            mySelect.selectedIndex = j;
            //console.log(">> Done defaulting option : "+temp);
            break;
        }
    }

    //dojo.byId('paymentType').value = ptType;
    //dijit.byId('capturePaymentWrapper_amount').attr("value", 0.00);
    if (pType == 'Cheque') {
        chequeDiv.className = 'showControl';
    } else if (pType == 'POS') {
        posDiv.className = 'showControl';
    } else if (pType == 'RTGS') {
        rtgsDiv.className = 'showControl';
    } else if (pType == 'Ecocash') {
        ecocashDiv.className = 'showControl';
    }else if (pType == 'SB-Ecocash') {
        ecocashDiv.className = 'showControl';
    } else if (pType == 'Voucher') {
        voucherDiv.className = 'showControl';
    }
}

function showSerialsDialog(response){
    var serialDiv = dojo.byId("serialsDiv");
    if(serialDiv){
        serialDiv.innerHTML= response;
        dijit.byId('serialsDialog').show();
    }
}

function getResponseUploadsTxnBatchId(response){
    console.log(">> In getResponseUploadsTxnBatchId "+response);
    if(strStartsWith(response,'CONTINUE_TO_UPLOADS')) {
        var arr = response.split("|");
        return arr[1];
    } else {
        alert(response);
        clearFormDetails();
        ecorecStandBy.hide();
        return 'error';
    }

}

function submitThisForm(toBeSubmitted){
    ecorecStandBy.show();
    toBeSubmitted.submit();
}

function submitPost(toBeSubmitted, thisButton){
    console.debug(">>>>>>>>> Button : "+thisButton.disabled);
    if(thisButton.disabled === false) {
        thisButton.disabled = true;
        ecorecStandBy.show();
        toBeSubmitted.submit();
    } else {
        //Do nothing
        console.debug(">>>>>>>>> Do nothing ");
    }
}

function clearFormDetails(){
    var stockItemCodeCombo = dojo.byId("stockItemCode");
    if(stockItemCodeCombo){
        stockItemCodeCombo.value = "";
    }
    var paymentTypeCombo = dojo.byId("paymentType");
    paymentTypeCombo.selectedIndex = 0;
    var currencyCombo = dojo.byId("currency");
    currencyCombo.selectedIndex = 0;

    var chequeType = dojo.byId("chequeType");
    chequeType.selectedIndex = 0;

    var chequeBank = dojo.byId("chequeBank");
    chequeBank.selectedIndex = 0;

    var rtgsBank = dojo.byId("rtgsBank");
    rtgsBank.selectedIndex = 0;

    var voucherType = dojo.byId("voucherType");
    voucherType.selectedIndex = 0;


    var quantity = dijit.byId("capturePaymentWrapper_quantity");
    if(quantity){
        quantity.attr("value", 1);
    }
    var paymentInputs = ['capturePaymentWrapper_chequeAccountNumber',
        'capturePaymentWrapper_chequeNumber',
        'capturePaymentWrapper_posRefNumber',
        'capturePaymentWrapper_rtgsRefNumber',
        'capturePaymentWrapper_voucherNumber',
        'capturePaymentWrapper_ecocashRefNumber'];
    for(var count=0; count < paymentInputs.length ; count++){
        var paymentInput = dijit.byId(paymentInputs[count]);
        paymentInput.attr("value", "");
    }

    var amount = dijit.byId("capturePaymentWrapper_amount");
    var amountValue = dojo.byId("amountRemaining").innerHTML;
    amountValue = amountValue.replace(/,/g,"").replace(/ /g,"");

    amount.attr("value", amountValue);

    var paymentType = document.getElementById("paymentType");
    var paymentTypeValue = paymentType.options[paymentType.selectedIndex].value;
    processPaymentTypeChange(paymentTypeValue);
}

function toggleVis(groupId){
    // the element that contains the area to hide/show
    toToggle = document.getElementById(groupId);

    // toggle the class attribute to change visibility
    currentClass = toToggle.className;
    if(currentClass == '' || currentClass == 'showControl')
        toToggle.className = 'hideControl';

    else
        toToggle.className = 'showControl';

}

function is_int(value){
    if((parseFloat(value) == parseInt(value)) && !isNaN(value)){
        return true;
    } else {
        return false;
    }
}

function isNotInt(value){
    if((parseFloat(value) == parseInt(value)) && !isNaN(value)){
        return false;
    } else {
        return true;
    }
}

function sendToSBPOS(paymentId, cashierId, amount, callback) {

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "http://127.0.0.1:9991/api/sale", true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.onreadystatechange = function () {
        if (this.readyState != 4) return;
        if (this.status == 200) {
            callback(this.responseText);
        }
    };
    xhr.send(JSON.stringify({
        "amount": amount*100,
        "tellerId" : cashierId,
        "tranKey" : paymentId
    }));
}

function sendToEcocash(paymentId, cashierId, amount, mobileNumber,callback) {
    var xhr = new XMLHttpRequest();
    xhr.open("POST", "http://127.0.0.1:9991/api/ecosale", true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.onreadystatechange = function () {
        if (this.readyState != 4) return;
        if (this.status == 200) {
            callback(JSON.parse(this.responseText));
        }
    };
    xhr.send(JSON.stringify({
        "amount": amount*100,
        "ecoPhoneNumber": mobileNumber,
        "tellerId" : cashierId,
        "tranKey" : paymentId
    }));
}

function reversePayment(paymentId, callback) {
    var xhr = new XMLHttpRequest();
    xhr.open("POST", "http://127.0.0.1:9991/api/reverse", true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.onreadystatechange = function () {
        if (this.readyState != 4) return;
        if (this.status == 200) {
            callback(JSON.parse(this.responseText));
        }
    };
    xhr.send(JSON.stringify({
        "tranKey" : paymentId
    }));
}

