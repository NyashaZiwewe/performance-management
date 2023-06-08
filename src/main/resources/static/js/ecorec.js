function toggleInputs(name, input) {
    let inputDoc = document.getElementById(name);
    let customerName = document.getElementById('customerName');
    let taxVatNumber = document.getElementById('taxVatNumber');
    let mobileNumber = document.getElementById('mobileNumber');

    if (input.checked) {
        inputDoc.className = "d-block";

         if(name === "customise_inputs"){
            customerName.setAttribute("required", "");
            taxVatNumber.setAttribute("required", "");
         }
         if(name === "freeOfCharge_inputs"){
             mobileNumber.setAttribute("required", "");
         }
    } else {
        inputDoc.className = "d-none";

        if(name === "customise_inputs"){
            customerName.removeAttribute("required");
            taxVatNumber.removeAttribute("required");
        }
        if(name === "freeOfCharge_inputs"){
            mobileNumber.removeAttribute("required");
        }
    }
}
function processPaymentTypeChange(pType) {
    console.log(pType);
    //get all payment types div
    let chequeDiv = document.getElementById('cheque_inputs');
    let posDiv = document.getElementById('pos_inputs');
    let rtgsDiv = document.getElementById('rtgs_inputs');
    let ecocashDiv = document.getElementById('ecocash_inputs');
    let sbEcocashDiv = document.getElementById('sb_ecocash_inputs');
    let voucherDiv = document.getElementById('voucher_inputs');
    //hide all
    chequeDiv.className = 'd-none';
    posDiv.className = 'd-none';
    rtgsDiv.className = 'd-none';
    ecocashDiv.className = 'd-none';
    sbEcocashDiv.className = 'd-none';
    voucherDiv.className = 'd-none';

    //show appropriate
    if (pType == 'Cheque') {
        chequeDiv.className = 'd-block';
    } else if (pType == 'POS') {
        posDiv.className = 'd-block';
    } else if (pType == 'RTGS') {
        rtgsDiv.className = 'd-block';
    } else if (pType == 'Ecocash') {
        ecocashDiv.className = 'd-block';
    }else if (pType == 'SB-Ecocash') {
        sbEcocashDiv.className = 'd-block';
    } else if (pType == 'Voucher') {
        voucherDiv.className = 'd-block';
    }
}
function confirmRemovePaymentType() {
    return confirm("Are you sure you want to remove the selected payment ?");
}
function confirmRemoveInvoiceLine() {
    return confirm("Are you sure you want to remove the selected stock item from the cart ?");
}
function processTransactionDetailsResponse(response){
    if(strStartsWith(response,'<div id="transaction" class="ibox-content">')){
        $('#stockSerialsModal').modal('hide');
        updateTransactionDetailsResponse(response);
    }else if(strStartsWith(response,'<div id="stockSerials">')){
        $("#stockSerials").html(response);
        $('#stockSerialsModal').modal('show');
    }else if(strStartsWith(response,'<div id="serialRanges">')) {
        $("#serialRanges").html(response);
        $('#serialRangesModal').modal('show');
    }else if(strStartsWith(response, '<div id="details" class="ibox-content">')){
        $('#details').html(response);
    }else if(strStartsWith(response, '<div id="signatureText" align="center" class="thermalReceiptClass" style="font-size: smaller; font-weight: bold">')){
        $('#signatureText').html(response);
    }else {
        $('.alerts').html(response);
    }
}
function updateTransactionDetailsResponse(response){
    $("#transaction").html(response);
}
function strStartsWith(str,prefix){
    return str.indexOf(prefix)==0;
}