function rePrint() {
    let fl = "false";
    let printer1 = new ActiveXObject("EltradeFPAx.EltradeFprn");
    let ciPrinter = new ActiveXObject("FPAX_TAN2000.FPAX_TAN200");
    let printer1Init;
    let lresult;
    let comport = 3;

    ciPrinter.INIT_MY_FP(comport, 115200, fl, fl, 2000);
    lresult = ciPrinter.CONNECT_TO_PRINTER();

    if (lresult == 0) {
        datecsRePrint(ciPrinter, lresult);
        lresult = ciPrinter.CLOSE_CONNECTION();
    } else if (lresult != 0) {
        lresult = ciPrinter.CLOSE_CONNECTION();
        printer1Init = printer1.Init(comport, 115200, 0, 3, 0, "");

        if (printer1Init == 0) {
            eltradeRePrint(printer1, comport, printer1Init);
        } else if (lresult != 0 && printer1Init != 0) {
            alert("check printer connection!!");
        }
    }
    return;
}

function eltradePrint(printer1, comport, printer1Init) {
    let paymentObjectDocument = document.getElementById("paymentObject").value;
    let paymentJsObject = null;
    if (paymentObjectDocument != null && paymentObjectDocument != "" && paymentObjectDocument != undefined) {
        paymentJsObject = eval('(' + paymentObjectDocument + ')');
    }
    let batchObjectDocument = document.getElementById("batchObject").value;
    let batchJsObject = null;
    if (batchObjectDocument != null && batchObjectDocument != "" && batchObjectDocument != undefined) {
        batchJsObject = eval('(' + batchObjectDocument + ')');
    }
    let expiryDocument = document.getElementById("expiry").value;
    let expiryDateObject = null;
    if (expiryDocument != null && expiryDocument != "" && expiryDocument != undefined) {
        expiryDateObject = eval('(' + expiryDocument + ')');
    }
    let transactionType = null;
    let type = null;
    if (batchJsObject != null) {
        transactionType = batchJsObject.transactionType;
        type = batchJsObject.type;
    } else {
        transactionType = paymentJsObject.transactionType;
    }
    let companyName = "Econet";
    let city = "Harare";
    let address = "2 Old Mutare Rd, Msasa, Harare";
    let invoiceVatNumber = "10003121";
    let bpn = "200002587";
    let invoiceTaxNumber = "200002587";
    let change = paymentJsObject.change;
    if (change == 0) {
        change = null;
    }
    let currency = paymentJsObject.currency;
    let changeCurrency = paymentJsObject.changeCurrency;
    let changeRate = paymentJsObject.changeRate;
    let comment = paymentJsObject.comment;
    let cashierName = paymentJsObject.cashierName;
    let accountNumber = paymentJsObject.customerAccountNumber;
    let accountName = paymentJsObject.customerName;
    let invoiceNumber = paymentJsObject.invoiceNumber;
    let refInvoiceNumber = paymentJsObject.refInvoiceNumber;
    let receiptNumber = paymentJsObject.receiptNumber;
    let mobileNumber = paymentJsObject.mobileNumber;
    let documentNumber = paymentJsObject.documentNumber;
    let depositSerialNumber = paymentJsObject.depositSerialNumber;
    let secDepAccountNumber = paymentJsObject.secDepAccountNumber;
    if (secDepAccountNumber != null && secDepAccountNumber != undefined && secDepAccountNumber != "") {
        accountNumber = null;
    }
    let customerVATNumber = paymentJsObject.customerVATNumber;
    let secDepAccountName = paymentJsObject.secDepAccountName;
    if (secDepAccountName != null && secDepAccountName != undefined && secDepAccountName != "") {
        accountName = null;
    }
    let invoiceReceiver = paymentJsObject.customerName;
    let amountDue = paymentJsObject.amountDue;
    let amountTendered = paymentJsObject.amountTendered;
    let signature = paymentJsObject.signature;
    let tenderedCurrency = paymentJsObject.tenderedCurrency;
    let price = paymentJsObject.unitPrice;
    let quantity = paymentJsObject.quantity;
    let description = paymentJsObject.description;
    type = paymentJsObject.type;
    amountDue = parseFloat(Math.round(amountDue * Math.pow(10, 2)) / Math.pow(10, 2));
    amountTendered = parseFloat(Math.round(amountTendered * Math.pow(10, 2)) / Math.pow(10, 2));
    let typeOfPayment = document.getElementById("secondSerialObjects").value;
    if (typeOfPayment == "POS") {
        typeOfPayment = "DEBIT CARD";
    }
    let jsonArrayString = document.getElementById("jsonArrayObject").value;
    let jsonObject = null;
    if (jsonArrayString != null && jsonArrayString != "" && jsonArrayString != undefined) {
        jsonObject = eval('(' + jsonArrayString + ')');
    }
    let serialObjectString = document.getElementById("serialObjects").value;
    let serialObject = null;
    if (serialObjectString != null && serialObjectString != "" && serialObjectString != undefined) {
        serialObject = eval('(' + serialObjectString + ')');
    }
    let rechargeCardObjectString = document.getElementById("rechargeCardObjects").value;
    let rechargeCardObject = null;
    if (rechargeCardObjectString != null && rechargeCardObjectString != "" && rechargeCardObjectString != undefined) {
        rechargeCardObject = eval('(' + rechargeCardObjectString + ')');
    }
    let isRechargeCard;
    let isBulkPurchase = 0;
    let cardSerial;
    let tax = paymentJsObject.tax;
    let fiscalType = 2;
    let isFoc;
    let taxGroup = 1;
    let isReturn;
    let total = paymentJsObject.total;
    let rtgsRefNumber = null;
    let posRefNumber = null;
    let chequeNumber = null;
    let chequeType = null;
    let bankSortCode = null;
    let discount;
    let upperDiscount = "";
    let lowerDiscount = "";
    let isSeriallyTracked = "";
    let updatedRechargeLength;
    let exeededLimit = "true";
    let loop = 30;
    let discountAllowed = null;
    let narrative = null;
    let paymentTypeString = document.getElementById("paymentTypeObjects").value;
    let paymentTypeObject = null;
    if (paymentTypeString != null && paymentTypeString != "" && paymentTypeString != undefined) {
        paymentTypeObject = eval('(' + paymentTypeString + ')');
    }

    if (jsonObject != null && jsonObject != "" && jsonObject != undefined) {
        for (let i = 0; i < jsonObject.length; i++) {
            let object = jsonObject[i];
            if ((!(object["paymentItemType"] == "Free Product" || object["paymentItemType"] == "Credit Sale")) && amountDue == 0) {
                isReturn = 1;
            }
            if (object["description"] == "Discount Allowed") {
                discountAllowed = object["amountToPay"];
                narrative = object["narrative"];
            }

            if (object["paymentItemType"] == "Bulk Purchase") {
                if (object["discountPercent"] == "9.00") {
                    upperDiscount = "true";
                } else if (object["discountPercent"] == "7.00") {
                    lowerDiscount = "true";
                }
            }
            if (object["paymentItemType"] == "Bulk Purchase" && parseFloat(object["amountToPay"]) < 0) {
                discount = object["amountToPay"];
            }
        }
    }

    if (rechargeCardObject != null && rechargeCardObject != "" && rechargeCardObject != undefined) {
        for (let i = 0; i < rechargeCardObject.length; i++) {
            let object = rechargeCardObject[i];

            if (object["cardKey"] != null || object["cardKey"] != "") {
                isRechargeCard = 1;
            }
        }
    }

    if (jsonObject != null && jsonObject != "" && jsonObject != undefined) {
        for (let i = 0; i < jsonObject.length; i++) {
            let object = jsonObject[i];
            if (object["isSeriallyTracked"] == "yes") {
                isSeriallyTracked = "yes";
                break;
            }
        }
    }

    if (serialObject != null && serialObject != "" && serialObject != undefined) {
        for (let i = 0; i < serialObject.length; i++) {
            let object = serialObject[i];
            if (object["lowerLimit"] != null && object["lowerLimit"] != "" && object["lowerLimit"] != undefined) {
                isBulkPurchase = 1;
            }
            cardSerial = object["cardSerial"];
        }
    }

    if (isReturn == 1) {
        amountDue = total;
    }

    if (invoiceNumber == null) {
        invoiceNumber = "";
    }

    if (type == 1) {
        if (printer1Init == 0) {
            printer1Init = printer1.StartBon(1, 1, "", invoiceNumber, invoiceTaxNumber, invoiceVatNumber, companyName, city, address, "MOL", invoiceReceiver);
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("Cashier       : " + cashierName, "", 1);
            }
            if (printer1Init == 0 && accountNumber != null) {
                printer1Init = printer1.AddLine("Account #     : " + accountNumber, "", 1);
            }
            if (printer1Init == 0 && secDepAccountNumber != null) {
                printer1Init = printer1.AddLine("Account #     : " + secDepAccountNumber, "", 1);
            }
            if (printer1Init == 0 && accountName != null) {
                printer1Init = printer1.AddLine("Account Name  : " + accountName, "", 1);
            }
            if (printer1Init == 0 && customerVATNumber != null) {
                printer1Init = printer1.AddLine("Customer VAT Number  : " + customerVATNumber, "", 1);
            }
            if (printer1Init == 0 && secDepAccountName != null) {
                printer1Init = printer1.AddLine("Account Name  : " + secDepAccountName, "", 1);
            }
            if (printer1Init == 0 && comment != null) {
                printer1Init = printer1.AddLine("REF           : " + comment, "", 1);
            }
            if (printer1Init == 0 && invoiceNumber != null && invoiceNumber != "") {
                printer1Init = printer1.AddLine("Invoice #     : " + invoiceNumber, "", 1);
            }
            if (printer1Init == 0 && refInvoiceNumber != null) {
                printer1Init = printer1.AddLine("Ref Invoice # : " + refInvoiceNumber, "", 1);
            }
            if (printer1Init == 0 && receiptNumber != null) {
                printer1Init = printer1.AddLine("Receipt Number: " + receiptNumber, "", 1);
            }
            if (printer1Init == 0 && transactionType != null) {
                printer1Init = printer1.AddLine("Description   : " + transactionType, "", 1);
            }
            if (printer1Init == 0 && mobileNumber != null) {
                printer1Init = printer1.AddLine("Mobile Number : " + mobileNumber, "", 1);
            }
            if (printer1Init == 0 && documentNumber != null) {
                printer1Init = printer1.AddLine("Document #    : " + documentNumber, "", 1);
            }
            if (printer1Init == 0 && tenderedCurrency != null) {
                printer1Init = printer1.AddLine("Tendered Currency    : " + tenderedCurrency, "", 1);
            }
            if (printer1Init == 0 && depositSerialNumber != null) {
                printer1Init = printer1.AddLine("Deposit Serial #   : " + depositSerialNumber, "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
            }
            if (amountDue != null && amountDue != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("Invoice Total :            " + tenderedCurrency + amountDue + "", "", 1);
                }
            }
            if (amountTendered != null && amountTendered != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("AMOUNT TENDERED :           " + tenderedCurrency + amountTendered + "", "", 1);
                }
            }
            if (change != null && change != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("CHANGE :                  " + changeCurrency + "" + change + "", "", 1);
                }
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
            }
            for (let i = 0; i < paymentTypeObject.length; i++) {
                let object = paymentTypeObject[i];
                if (object["paymentTypeString"] == "POS") {
                    object["paymentTypeString"] = "DEBIT CARD";
                }
                if (object["paymentTypeString"] == "DEBIT CARD") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "             " + object["currency"] + " " + object["amount"], "", 1);
                    }
                    if (object["posRefNumber"] != null && object["posRefNumber"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (POS Ref Number: " + object["posRefNumber"] + ")", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(" ", "", 1);
                        }
                    }
                }
                else if (object["paymentTypeString"] == "CHEQUE") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                 " + object["currency"] + " " + object["amount"], "", 1);
                    }
                    if (object["chequeType"] != null && object["chequeType"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (Cheque Type   : " + object["chequeType"] + ")", "", 1);
                        }

                    }
                    if (object["chequeNumber"] != null && object["chequeNumber"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (Cheque Number : " + object["chequeNumber"] + ")", "", 1);
                        }
                    }
                    if (object["bank"] != null && object["bank"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (Bank: " + object["bank"] + ")", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(" ", "", 1);
                        }
                    }
                }
                else if (object["paymentTypeString"] == "CASH") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                    }
                }
                else if (object["paymentTypeString"] == "CASH-RTGS") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                    }
                }
                else if (object["paymentTypeString"] == "RTGS") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                    }
                    if (object["rtgsRefNumber"] != null && object["rtgsRefNumber"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (RTGS Ref Number: " + object["rtgsRefNumber"] + ")", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(" ", "", 1);
                        }
                    }
                }
                else if (object["paymentTypeString"] == "ECOCASH") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                 " + object["currency"] + " " + object["amount"], "", 1);
                    }
                    if (object["ecocashRefNumber"] != null && object["ecocashRefNumber"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (ECOCASH Ref Number: " + object["ecocashRefNumber"] + ")", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(" ", "", 1);
                        }
                    }
                }
            }

            if (printer1Init == 0 && signature != null) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0 && signature != null) {
                printer1Init = printer1.AddLine(signature, "", 1);
            }
            if (printer1Init == 0 && signature != null) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" PLEASE RETAIN AS PROOF OF PAYMENT ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) printer1Init = printer1.EndBon();
            printer1.Close();
        }
    }
    else if (type == 1 && isReturn == 1) {
        printer1Init = printer1.StartBon(1, 1, "", invoiceNumber, invoiceTaxNumber, invoiceVatNumber, companyName, city, address, "MOL", invoiceReceiver);
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("              [RETURN]", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("Cashier       : " + cashierName, "", 1);
        }
        if (printer1Init == 0 && accountNumber != null) {
            printer1Init = printer1.AddLine("Account #     : " + accountNumber, "", 1);
        }
        if (printer1Init == 0 && secDepAccountNumber != null) {
            printer1Init = printer1.AddLine("Account #     : " + secDepAccountNumber, "", 1);
        }
        if (printer1Init == 0 && accountName != null) {
            printer1Init = printer1.AddLine("Account Name  : " + accountName, "", 1);
        }
        if (printer1Init == 0 && customerVATNumber != null) {
            printer1Init = printer1.AddLine("Customer VAT Number  : " + customerVATNumber, "", 1);
        }
        if (printer1Init == 0 && secDepAccountName != null) {
            printer1Init = printer1.AddLine("Account Name  : " + secDepAccountName, "", 1);
        }
        if (printer1Init == 0 && comment != null) {
            printer1Init = printer1.AddLine("REF           : " + comment, "", 1);
        }
        if (printer1Init == 0 && invoiceNumber != null && invoiceNumber != "") {
            printer1Init = printer1.AddLine("Invoice #     : " + invoiceNumber, "", 1);
        }
        if (printer1Init == 0 && refInvoiceNumber != null) {
            printer1Init = printer1.AddLine("Ref Invoice # : " + refInvoiceNumber, "", 1);
        }
        if (printer1Init == 0 && receiptNumber != null) {
            printer1Init = printer1.AddLine("Receipt Number: " + receiptNumber, "", 1);
        }
        if (printer1Init == 0 && transactionType != null) {
            printer1Init = printer1.AddLine("Description   : " + transactionType, "", 1);
        }
        if (printer1Init == 0 && mobileNumber != null) {
            printer1Init = printer1.AddLine("Mobile Number : " + mobileNumber, "", 1);
        }
        if (printer1Init == 0 && documentNumber != null) {
            printer1Init = printer1.AddLine("Document #    : " + documentNumber, "", 1);
        }
        if (printer1Init == 0 && tenderedCurrency != null) {
            printer1Init = printer1.AddLine("Tendered Currency    : " + tenderedCurrency, "", 1);
        }
        if (printer1Init == 0 && depositSerialNumber != null) {
            printer1Init = printer1.AddLine("Deposit Serial #   : " + depositSerialNumber, "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("------------------------------------ ", "", 1);
        }

        if (jsonObject != null || jsonObject != undefined) {
            for (let i = 0; i < jsonObject.length; i++) {
                let object = jsonObject[i];
                if (object["paymentItemType"] == "Free Product" || object["paymentItemType"] == "Credit Sale") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["description"], "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      " + object["quantity"] + " x       " + object["unitPrice"] + "            " + tenderedCurrency + object["subTotal"], "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("DISCOUNT" + "                      " + tenderedCurrency + "-" + object["subTotal"], "", 1);
                    }

                } else {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["description"], "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("        " + object["quantity"] + " x       " + object["unitPrice"] + "       " + tenderedCurrency + object["subTotal"], "", 1);
                    }
                }
            }
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("----------------------------------", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("TAXBL1" + "                       " + tenderedCurrency + total, "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("TAX1 14.50%" + "                  " + tenderedCurrency + tax, "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("----------------------------------", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("TOTAL" + "                        " + tenderedCurrency + total, "", 1);
        }
        if (change != null) {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("CHANGE" + "                      " + changeCurrency + "" + change, "", 1);
            }
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("ITEM #" + "                        " + jsonObject.length, "", 1);
        }

        for (let i = 0; i < paymentTypeObject.length; i++) {
            let object = paymentTypeObject[i];
            if (object["posRefNumber"] != null && object["posRefNumber"] != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("      (POS Ref Number: " + object["posRefNumber"] + ")", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(" ", "", 1);
                }
            }
            if (object["chequeType"] != null && object["chequeType"] != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("      (Cheque Type   : " + object["chequeType"] + ")", "", 1);
                }
            }
            if (object["chequeNumber"] != null && object["chequeNumber"] != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("      (Cheque Number : " + object["chequeNumber"] + ")", "", 1);
                }
            }
            if (object["bank"] != null && object["bank"] != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("      (Bank: " + object["bank"] + ")", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(" ", "", 1);
                }
            }
            if (object["rtgsRefNumber"] != null && object["rtgsRefNumber"] != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("      (RTGS Ref Number: " + object["rtgsRefNumber"] + ")", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(" ", "", 1);
                }
            }
            if (object["ecocashRefNumber"] != null && object["ecocashRefNumber"] != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("      (ECOCASH Ref Number: " + object["ecocashRefNumber"] + ")", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(" ", "", 1);
                }
            }
            if (object["voucherNumber"] != null && object["voucherNumber"] != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("      (VOUCHER Number: " + object["voucherNumber"] + ")", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(" ", "", 1);
                }
            }
            if (object["voucherType"] != null && object["voucherType"] != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("      (VOUCHER Type: " + object["voucherType"] + ")", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(" ", "", 1);
                }
            }
        }

        if (isBulkPurchase == 1) {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("  ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("                Serials", "", 1);
            }
            for (let i = 0; i < serialObject.length; i++) {
                let object = serialObject[i];
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["stockItemCode"] + ": " + object["lowerLimit"] + "-" + object["upperLimit"], "", 1);
                }

            }
        }

        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(signature, "", 1);
        }
        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" PLEASE RETAIN AS PROOF OF PAYMENT ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) printer1Init = printer1.EndBon();
        printer1.Close();

        if (isRechargeCard == 1) {
            updatedRechargeLength = rechargeCardObject.length;
            while (exeededLimit == "true") {
                printer1Init = printer1.StartBon(1, 1, "", invoiceNumber, invoiceTaxNumber, invoiceVatNumber, companyName, city, address, "MOL", invoiceReceiver);
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(" ", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("Cashier       : " + cashierName, "", 1);
                }
                if (printer1Init == 0 && accountNumber != null) {
                    printer1Init = printer1.AddLine("Account #     : " + accountNumber, "", 1);
                }
                if (printer1Init == 0 && secDepAccountNumber != null) {
                    printer1Init = printer1.AddLine("Account #     : " + secDepAccountNumber, "", 1);
                }
                if (printer1Init == 0 && accountName != null) {
                    printer1Init = printer1.AddLine("Account Name  : " + accountName, "", 1);
                }
                if (printer1Init == 0 && customerVATNumber != null) {
                    printer1Init = printer1.AddLine("Customer VAT Number  : " + customerVATNumber, "", 1);
                }
                if (printer1Init == 0 && secDepAccountName != null) {
                    printer1Init = printer1.AddLine("Account Name  : " + secDepAccountName, "", 1);
                }
                if (printer1Init == 0 && comment != null) {
                    printer1Init = printer1.AddLine("REF           : " + comment, "", 1);
                }
                if (printer1Init == 0 && invoiceNumber != null && invoiceNumber != "") {
                    printer1Init = printer1.AddLine("Invoice #     : " + invoiceNumber, "", 1);
                }
                if (printer1Init == 0 && refInvoiceNumber != null) {
                    printer1Init = printer1.AddLine("Ref Invoice # : " + refInvoiceNumber, "", 1);
                }
                if (printer1Init == 0 && receiptNumber != null) {
                    printer1Init = printer1.AddLine("Receipt Number: " + receiptNumber, "", 1);
                }
                if (printer1Init == 0 && transactionType != null) {
                    printer1Init = printer1.AddLine("Description   : " + transactionType, "", 1);
                }
                if (printer1Init == 0 && mobileNumber != null) {
                    printer1Init = printer1.AddLine("Mobile Number : " + mobileNumber, "", 1);
                }
                if (printer1Init == 0 && documentNumber != null) {
                    printer1Init = printer1.AddLine("Document #    : " + documentNumber, "", 1);
                }
                if (printer1Init == 0 && tenderedCurrency != null) {
                    printer1Init = printer1.AddLine("Tendered Currency    : " + tenderedCurrency, "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("------------------------------------ ", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("", "", 1);
                }
                for (let i = 0; i < loop; i++) {
                    if (rechargeCardObject[i] != null) {

                        let object = rechargeCardObject[i];
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("Card Key: ", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(object["cardKey"], "BOLD", 1);
                        }

                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("Expiry Date: " + expiryDateObject.expiryDate, "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("S/N: " + object["cardSerial"], "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("Batch Id: " + object["cardBatch"], "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("Denomination: " + "$" + object["denomination"], "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(object["rechargeInstruction"] + " Dial 111 for Customer Care or sms help to 111", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(" ", "", 1);
                        }
                    }
                    rechargeCardObject[i] = null;
                }
                loop = loop + 30;
                updatedRechargeLength = updatedRechargeLength - 30;
                if (updatedRechargeLength == 0 || updatedRechargeLength < 0) {
                    exeededLimit = "false";
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("----------------------------------", "", 1);
                }
                if (printer1Init == 0) printer1Init = printer1.EndBon();
            }
            printer1.Close();
        }

    }
    else if (type == 2) {
        if (printer1Init == 0) {
            printer1Init = printer1.StartBon(2, 1, "", invoiceNumber, invoiceTaxNumber, invoiceVatNumber, companyName, city, address, "MOL", invoiceReceiver);
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("Cashier       : " + cashierName, "", 1);
            }
            if (printer1Init == 0 && accountNumber != null) {
                printer1Init = printer1.AddLine("Account #     : " + accountNumber, "", 1);
            }
            if (printer1Init == 0 && secDepAccountNumber != null) {
                printer1Init = printer1.AddLine("Account #     : " + secDepAccountNumber, "", 1);
            }
            if (printer1Init == 0 && accountName != null) {
                printer1Init = printer1.AddLine("Account Name  : " + accountName, "", 1);
            }
            if (printer1Init == 0 && customerVATNumber != null) {
                printer1Init = printer1.AddLine("Customer VAT Number  : " + customerVATNumber, "", 1);
            }
            if (printer1Init == 0 && secDepAccountName != null) {
                printer1Init = printer1.AddLine("Account Name  : " + secDepAccountName, "", 1);
            }
            if (printer1Init == 0 && comment != null) {
                printer1Init = printer1.AddLine("REF           : " + comment, "", 1);
            }
            if (printer1Init == 0 && invoiceNumber != null && invoiceNumber != "") {
                printer1Init = printer1.AddLine("Invoice #     : " + invoiceNumber, "", 1);
            }
            if (printer1Init == 0 && refInvoiceNumber != null) {
                printer1Init = printer1.AddLine("Ref Invoice # : " + refInvoiceNumber, "", 1);
            }
            if (printer1Init == 0 && receiptNumber != null) {
                printer1Init = printer1.AddLine("Receipt Number: " + receiptNumber, "", 1);
            }
            if (printer1Init == 0 && transactionType != null) {
                printer1Init = printer1.AddLine("Description   : " + transactionType, "", 1);
            }
            if (printer1Init == 0 && mobileNumber != null) {
                printer1Init = printer1.AddLine("Mobile Number : " + mobileNumber, "", 1);
            }
            if (printer1Init == 0 && documentNumber != null) {
                printer1Init = printer1.AddLine("Document #    : " + documentNumber, "", 1);
            }
            if (printer1Init == 0 && tenderedCurrency != null) {
                printer1Init = printer1.AddLine("Tendered Currency    : " + tenderedCurrency, "", 1);
            }
            if (printer1Init == 0 && depositSerialNumber != null) {
                printer1Init = printer1.AddLine("Deposit Serial #   : " + depositSerialNumber, "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("------------------------------------ ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                if (jsonObject != null || jsonObject != undefined) {
                    for (let i = 0; i < jsonObject.length; i++) {
                        let object = jsonObject[i];

                        if (object["paymentItemType"] == "Free Product" || object["paymentItemType"] == "Credit Sale") {
                            isFoc = 1;
                            if (object["description"] == "Discount Allowed") {

                            } else {
                                let discountPrice = object["unitPrice"] * 100;
                                if (printer1Init == 0) {
                                    printer1Init = printer1.AddPlu(1, object["description"], object["unitPrice"], object["quantity"], taxGroup);
                                }
                                if (printer1Init == 0) {
                                    printer1Init = printer1.AddDiscount(1, -discountPrice);
                                }
                            }
                        }
                        else {
                            if (object["description"] == "Discount Allowed") {
                            } else {
                                if (printer1Init == 0) {
                                    printer1Init = printer1.AddPlu("1", object["description"], parseFloat(object["unitPrice"]), parseFloat(object["quantity"]), taxGroup);
                                }
                            }
                        }
                    }
                    if (discountAllowed != null) {
                        if (narrative == "7.0%") {
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddDiscount(2, -7.0);
                            }
                            discountAllowed = null;
                        } else if (narrative == "9.0%") {
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddDiscount(2, -9.0);
                            }
                            discountAllowed = null;
                        }
                    }
                }
            }

            if (isFoc == 1) {
                if (printer1Init == 0) printer1Init = printer1.AddPayment(1, "CASH", amountDue, 1);
            }
            else {
                if (printer1Init == 0) printer1Init = printer1.AddPayment(1, "TOTAL TENDERED", amountTendered, 1);
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
                }
                for (let i = 0; i < paymentTypeObject.length; i++) {
                    let object = paymentTypeObject[i];
                    if (object["paymentTypeString"] == "POS") {
                        object["paymentTypeString"] = "DEBIT CARD";
                    }
                    if (object["paymentTypeString"] == "DEBIT CARD") {
                        //debitTotal = debitTotal + parseFloat(object["amount"]);
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("DEBIT CARD" + "                 " + object["currency"] + " " + object["amount"], "", 1);
                        }
                        if (object["posRefNumber"] != null && object["posRefNumber"] != "") {
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("      (POS Ref Number: " + object["posRefNumber"] + ")", "", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine(" ", "", 1);
                            }
                        }
                    }
                    else if (object["paymentTypeString"] == "CHEQUE") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("CHEQUE" + "                   " + object["currency"] + " " + object["amount"], "", 1);
                        }
                        if (object["chequeType"] != null && object["chequeType"] != "") {
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("      (Cheque Type   : " + object["chequeType"] + ")", "", 1);
                            }
                        }
                        if (object["chequeNumber"] != null && object["chequeNumber"] != "") {
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("      (Cheque Number : " + object["chequeNumber"] + ")", "", 1);
                            }
                        }
                        if (object["bank"] != null && object["bank"] != "") {
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("      (Bank: " + object["bank"] + ")", "", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine(" ", "", 1);
                            }
                        }
                    }
                    else if (object["paymentTypeString"] == "CASH") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("CASH" + "                     " + object["currency"] + " " + object["amount"], "", 1);
                        }
                    }
                    else if (object["paymentTypeString"] == "CASH-RTGS") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("CASH-RTGS" + "                     " + object["currency"] + " " + object["amount"], "", 1);
                        }
                    }
                    else if (object["paymentTypeString"] == "RTGS") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("RTGS" + "                     " + object["currency"] + " " + object["amount"], "", 1);
                        }
                        if (object["rtgsRefNumber"] != null && object["rtgsRefNumber"] != "") {
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("      (RTGS Ref Number: " + object["rtgsRefNumber"] + ")", "", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine(" ", "", 1);
                            }
                        }
                    }
                    else if (object["paymentTypeString"] == "ECOCASH") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("ECOCASH" + "                  " + object["currency"] + " " + object["amount"], "", 1);
                        }
                        if (object["ecocashRefNumber"] != null && object["ecocashRefNumber"] != "") {
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("      (ECOCASH Ref Number: " + object["ecocashRefNumber"] + ")", "", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine(" ", "", 1);
                            }
                        }
                    }

                    if (change != null && change != "" && changeCurrency != "USD") {
                        //if (printer1Init==0){printer1Init = printer1.AddLine("CHANGE RATE :              "+changeRate+"", "", 1);}
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("CHANGE :                 " + changeCurrency + "" + change + "", "BOLD", 1);
                        }
                    }
                }
            }

            if (isSeriallyTracked == "yes" && isBulkPurchase != 1) {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("----------------------------------", "", 1);
                }
                for (let i = 0; i < serialObject.length; i++) {
                    let object = serialObject[i];
                    //alert(object["serialNumber"]);
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("  ", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("Description: " + object["stockItemCode"], "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("Serial Number: " + object["serialNumber"], "", 1);
                    }

                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("----------------------------------", "", 1);
                }
            }

            if (isBulkPurchase == 1) {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("  ", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("                Serials", "", 1);
                }
                for (let i = 0; i < serialObject.length; i++) {
                    let object = serialObject[i];

                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["stockItemCode"] + ": " + object["lowerLimit"] + "-" + object["upperLimit"], "", 1);
                    }

                }
            }

            if (printer1Init == 0 && signature != null) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0 && signature != null) {
                printer1Init = printer1.AddLine(signature, "", 1);
            }
            if (printer1Init == 0 && signature != null) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" PLEASE RETAIN AS PROOF OF PAYMENT ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }

            if (printer1Init == 0) printer1Init = printer1.EndBon();
            printer1.Close();

            if (isRechargeCard == 1) {
                updatedRechargeLength = rechargeCardObject.length;
                while (exeededLimit == "true") {
                    let printer1 = new ActiveXObject("EltradeFPAx.EltradeFprn");
                    printer1Init = printer1.Init(comport, 115200, 0, 3, 0, "");

                    printer1Init = printer1.StartBon(1, 1, "", invoiceNumber, invoiceTaxNumber, invoiceVatNumber, companyName, city, address, "MOL", invoiceReceiver);
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("Cashier       : " + cashierName, "", 1);
                    }
                    if (printer1Init == 0 && accountNumber != null) {
                        printer1Init = printer1.AddLine("Account #     : " + accountNumber, "", 1);
                    }
                    if (printer1Init == 0 && secDepAccountNumber != null) {
                        printer1Init = printer1.AddLine("Account #     : " + secDepAccountNumber, "", 1);
                    }
                    if (printer1Init == 0 && accountName != null) {
                        printer1Init = printer1.AddLine("Account Name  : " + accountName, "", 1);
                    }
                    if (printer1Init == 0 && customerVATNumber != null) {
                        printer1Init = printer1.AddLine("Customer VAT Number  : " + customerVATNumber, "", 1);
                    }
                    if (printer1Init == 0 && secDepAccountName != null) {
                        printer1Init = printer1.AddLine("Account Name  : " + secDepAccountName, "", 1);
                    }
                    if (printer1Init == 0 && comment != null) {
                        printer1Init = printer1.AddLine("REF           : " + comment, "", 1);
                    }
                    if (printer1Init == 0 && invoiceNumber != null && invoiceNumber != "") {
                        printer1Init = printer1.AddLine("Invoice #     : " + invoiceNumber, "", 1);
                    }
                    if (printer1Init == 0 && refInvoiceNumber != null) {
                        printer1Init = printer1.AddLine("Ref Invoice # : " + refInvoiceNumber, "", 1);
                    }
                    if (printer1Init == 0 && receiptNumber != null) {
                        printer1Init = printer1.AddLine("Receipt Number: " + receiptNumber, "", 1);
                    }
                    if (printer1Init == 0 && transactionType != null) {
                        printer1Init = printer1.AddLine("Description   : " + transactionType, "", 1);
                    }
                    if (printer1Init == 0 && mobileNumber != null) {
                        printer1Init = printer1.AddLine("Mobile Number : " + mobileNumber, "", 1);
                    }
                    if (printer1Init == 0 && documentNumber != null) {
                        printer1Init = printer1.AddLine("Document #    : " + documentNumber, "", 1);
                    }
                    if (printer1Init == 0 && tenderedCurrency != null) {
                        printer1Init = printer1.AddLine("Tendered Currency    : " + tenderedCurrency, "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("------------------------------------ ", "", 1);
                    }

                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("", "", 1);
                    }

                    for (let i = 0; i < loop; i++) {
                        if (rechargeCardObject[i] != null) {

                            let object = rechargeCardObject[i];
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("Card Key: ", "", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine(object["cardKey"], "BOLD", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("Expiry Date: " + expiryDateObject.expiryDate, "", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("S/N: " + object["serialNumber"], "", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("Batch Id: " + object["batchNumber"], "", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine("Denomination: " + "$" + object["denomination"], "", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine(object["rechargeInstruction"] + " Dial 111 for Customer Care or sms help to 111", "", 1);
                            }
                            if (printer1Init == 0) {
                                printer1Init = printer1.AddLine(" ", "", 1);
                            }
                        }
                        rechargeCardObject[i] = null;
                    }
                    loop = loop + 30;
                    updatedRechargeLength = updatedRechargeLength - 30;
                    if (updatedRechargeLength == 0 || updatedRechargeLength < 0) {
                        exeededLimit = "false";
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("----------------------------------", "", 1);
                    }

                    if (printer1Init == 0) printer1Init = printer1.EndBon();
                    printer1.Close();
                }

            }

        }
        else {
            alert(printer1.GetLastError(1));
        }
    }
    else if (type == 3) {
        if (printer1Init == 0) {
            currency = batchJsObject.currency;
            changeCurrency = batchJsObject.changeCurrency;
            printer1Init = printer1.StartBon(1, 1, "", "", "", "", companyName, city, address, "MOL", "");
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("Cashier       : " + batchJsObject.cashier, "", 1);
            }
            if (printer1Init == 0 && transactionType != null) {
                printer1Init = printer1.AddLine("Description   : " + batchJsObject.transactionType, "", 1);
            }
            if (printer1Init == 0 && batchJsObject.batchDocumentNumber != null) {
                printer1Init = printer1.AddLine("Document #    : " + batchJsObject.batchDocumentNumber, "", 1);
            }
            if (printer1Init == 0 && tenderedCurrency != null) {
                printer1Init = printer1.AddLine("Tendered Currency    : " + tenderedCurrency, "", 1);
            }
            if (printer1Init == 0 && batchJsObject.batchCount != null) {
                printer1Init = printer1.AddLine("Payments Count #   : " + batchJsObject.batchCount, "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
            }
            if (batchJsObject.batchTotalAmount != null && batchJsObject.batchTotalAmount != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("Invoice Total :            " + currency + batchJsObject.batchTotalAmount + "", "", 1);
                }
            }
            if (batchJsObject.tenderedAmount != null && batchJsObject.tenderedAmount != "") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("AMOUNT TENDERED :           " + currency + batchJsObject.tenderedAmount + "", "", 1);
                }
            }
            change = batchJsObject.tenderedAmount - batchJsObject.batchTotalAmount;

            if (change != null && change != "" && change > 0) {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("CHANGE :                  " + changeCurrency + change + "", "", 1);
                }
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
            }

            for (let i = 0; i < paymentTypeObject.length; i++) {
                let object = paymentTypeObject[i];
                if (object["paymentTypeString"] == "POS") {
                    object["paymentTypeString"] = "DEBIT CARD";
                }
                if (object["paymentTypeString"] == "DEBIT CARD") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "             " + object["currency"] + " " + object["amount"], "", 1);
                    }
                    if (object["posRefNumber"] != null && object["posRefNumber"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (POS Ref Number: " + object["posRefNumber"] + ")", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(" ", "", 1);
                        }
                    }
                }
                else if (object["paymentTypeString"] == "CHEQUE") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                 " + object["currency"] + " " + object["amount"], "", 1);
                    }
                    if (object["chequeType"] != null && object["chequeType"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (Cheque Type   : " + object["chequeType"] + ")", "", 1);
                        }

                    }
                    if (object["chequeNumber"] != null && object["chequeNumber"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (Cheque Number : " + object["chequeNumber"] + ")", "", 1);
                        }
                    }
                    if (object["bank"] != null && object["bank"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (Bank: " + object["bank"] + ")", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(" ", "", 1);
                        }
                    }
                }
                else if (object["paymentTypeString"] == "CASH") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                    }
                }
                else if (object["paymentTypeString"] == "CASH-RTGS") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                    }
                }
                else if (object["paymentTypeString"] == "RTGS") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                    }
                    if (object["rtgsRefNumber"] != null && object["rtgsRefNumber"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (RTGS Ref Number: " + object["rtgsRefNumber"] + ")", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(" ", "", 1);
                        }
                    }
                }
                else if (object["paymentTypeString"] == "ECOCASH") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                 " + object["currency"] + " " + object["amount"], "", 1);
                    }
                    if (object["ecocashRefNumber"] != null && object["ecocashRefNumber"] != "") {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("      (ECOCASH Ref Number: " + object["ecocashRefNumber"] + ")", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(" ", "", 1);
                        }
                    }
                }
            }

            if (printer1Init == 0 && signature != null) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0 && signature != null) {
                printer1Init = printer1.AddLine(signature, "", 1);
            }
            if (printer1Init == 0 && signature != null) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" PLEASE RETAIN AS PROOF OF PAYMENT ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine(" ", "", 1);
            }
            if (printer1Init == 0) printer1Init = printer1.EndBon();
            printer1.Close();
        }
    }
}

function eltradeRePrint(printer1, comport, printer1Init) {
    console.log("in eltradeRePrint");
    let paymentObjectDocument = document.getElementById("paymentObject").value;
    let paymentJsObject = null;
    if (paymentObjectDocument != null && paymentObjectDocument != "" && paymentObjectDocument != undefined) {
        paymentJsObject = eval('(' + paymentObjectDocument + ')');
    }

    let batchObjectDocument = document.getElementById("batchObject").value;
    let batchJsObject = null;
    if (batchObjectDocument != null && batchObjectDocument != "" && batchObjectDocument != undefined) {
        batchJsObject = eval('(' + batchObjectDocument + ')');
    }

    let expiryDocument = document.getElementById("expiry").value;
    let expiryDateObject = null;
    if (expiryDocument != null && expiryDocument != "" && expiryDocument != undefined) {
        expiryDateObject = eval('(' + expiryDocument + ')');
    }
    let change = paymentJsObject.change;
    if (change == 0) {
        change = null;
    }
    let currency = paymentJsObject.currency;
    let changeCurrency = paymentJsObject.changeCurrency;
    let changeRate = paymentJsObject.changeRate;
    let comment = paymentJsObject.comment;
    let cashierName = paymentJsObject.cashierName;
    let accountNumber = paymentJsObject.customerAccountNumber;
    let accountName = paymentJsObject.customerName;
    let invoiceNumber = paymentJsObject.invoiceNumber;
    let refInvoiceNumber = paymentJsObject.refInvoiceNumber;
    let receiptNumber = paymentJsObject.receiptNumber;
    let mobileNumber = paymentJsObject.mobileNumber;
    let documentNumber = paymentJsObject.documentNumber;
    let depositSerialNumber = paymentJsObject.depositSerialNumber;
    let transactionType = paymentJsObject.transactionType;
    let secDepAccountNumber = paymentJsObject.secDepAccountNumber;
    if (secDepAccountNumber != null && secDepAccountNumber != undefined && secDepAccountNumber != "") {
        accountNumber = null;
    }
    let customerVATNumber = paymentJsObject.customerVATNumber;
    let secDepAccountName = paymentJsObject.secDepAccountName;
    if (secDepAccountName != null && secDepAccountName != undefined && secDepAccountName != "") {
        accountName = null;
    }
    let invoiceTaxNumber = "200002587";
    let invoiceReceiver = paymentJsObject.customerName;
    let amountDue = paymentJsObject.amountDue;
    let amountTendered = paymentJsObject.amountTendered;
    amountDue = parseFloat(Math.round(amountDue * Math.pow(10, 2)) / Math.pow(10, 2));
    amountTendered = parseFloat(Math.round(amountTendered * Math.pow(10, 2)) / Math.pow(10, 2));
    let signature = paymentJsObject.signature;
    let tenderedCurrency = paymentJsObject.tenderedCurrency;
    console.log("ESD Signature : " + signature);
    console.log("ESD tendered Currency : " + tenderedCurrency);
    let typeOfPayment = document.getElementById("secondSerialObjects").value;
    if (typeOfPayment == "POS") {
        typeOfPayment = "DEBIT CARD";
    }

    let price = paymentJsObject.unitPrice;
    let quantity = paymentJsObject.quantity;
    let description = paymentJsObject.description;
    let type = paymentJsObject.type;
    let companyName = "Econet";
    let city = "Harare";
    let address = "2 Old Mutare Rd, Msasa, Harare";
    let invoiceVatNumber = "10003121";
    let bpn = "200002587";
    let paymentTypeString = document.getElementById("paymentTypeObjects").value;
    let paymentTypeObject = null;
    if (paymentTypeString != null && paymentTypeString != "" && paymentTypeString != undefined) {
        paymentTypeObject = eval('(' + paymentTypeString + ')');
    }
    let jsonArrayString = document.getElementById("jsonArrayObject").value;
    let jsonObject = null;
    if (jsonArrayString != null && jsonArrayString != "" && jsonArrayString != undefined) {
        jsonObject = eval('(' + jsonArrayString + ')');
    }
    console.log(jsonArrayString);
    let serialObjectString = document.getElementById("serialObjects").value;
    let serialObject = null;
    if (serialObjectString != null && serialObjectString != "" && serialObjectString != undefined) {
        serialObject = eval('(' + serialObjectString + ')');
    }
    let rechargeCardObjectString = document.getElementById("rechargeCardObjects").value;
    let rechargeCardObject = null;
    if (rechargeCardObjectString != null && rechargeCardObjectString != "" && rechargeCardObjectString != undefined) {
        rechargeCardObject = eval('(' + rechargeCardObjectString + ')');
    }
    let isRechargeCard;
    let isBulkPurchase = 0;
    let cardSerial;
    let tax = paymentJsObject.tax;
    let fiscalType = 2;
    let isFoc;
    let taxGroup = 1;
    let isReturn;
    let total = paymentJsObject.total;
    let rtgsRefNumber = null;
    let posRefNumber = null;
    let chequeNumber = null;
    let chequeType = null;
    let bankSortCode = null;
    let discount;
    let cashTotal = 0;
    let chequeTotal = 0;
    let rtgsTotal = 0;
    let debitTotal = 0;
    let ecocashTotal = 0;
    let itemNumber;
    let upperDiscount = "";
    let lowerDiscount = "";
    let isSeriallyTracked = "";
    let updatedRechargeLength;
    let exeededLimit = "true";
    let loop = 30;
    if (jsonObject != null && jsonObject != "" && jsonObject != undefined) {
        for (let i = 0; i < jsonObject.length; i++) {
            let object = jsonObject[i];
            itemNumber = jsonObject.length;
        }
    }
    if (jsonObject != null && jsonObject != "" && jsonObject != undefined) {
        for (let i = 0; i < jsonObject.length; i++) {
            let object = jsonObject[i];

            if ((!(object["paymentItemType"] == "Free Product" || object["paymentItemType"] == "Credit Sale")) && amountDue == 0) {
                isReturn = 1;
            }
            if (object["paymentItemType"] == "Bulk Purchase" && parseFloat(object["amountToPay"]) < 0) {
                discount = object["amountToPay"];
                itemNumber = itemNumber - 1;
            }
            if (object["paymentItemType"] == "Bulk Purchase") {

                if (object["discountPercent"] == "9.00") {
                    upperDiscount = "true";
                    discountPercent = "-9%";
                } else if (object["discountPercent"] == "7.00") {
                    lowerDiscount = "true";
                    discountPercent = "-7%";
                }
            }
        }
    }
    if (rechargeCardObject != null && rechargeCardObject != "" && rechargeCardObject != undefined) {
        for (let i = 0; i < rechargeCardObject.length; i++) {
            let object = rechargeCardObject[i];
            if (object["cardKey"] != null || object["cardKey"] != "") {
                isRechargeCard = 1;
            }
        }
    }
    if (jsonObject != null && jsonObject != "" && jsonObject != undefined) {
        for (let i = 0; i < jsonObject.length; i++) {
            let object = jsonObject[i];
            if (object["isSeriallyTracked"] == "yes") {
                isSeriallyTracked = "yes";
                break;
            }
        }
    }
    if (serialObject != null && serialObject != "" && serialObject != undefined) {
        for (let i = 0; i < serialObject.length; i++) {
            if(serialObject[i] == null){
                break;
            }
            let object = serialObject[i];
            if (object["lowerLimit"] != null && object["lowerLimit"] != "" && object["lowerLimit"] != undefined) {
                isBulkPurchase = 1;
            }
            cardSerial = object["cardSerial"];
        }
    }
    if (isReturn == 1) {
        amountDue = total;
    }
    amountDue = parseFloat(Math.round(parseInt(amountDue) * Math.pow(10, 2)) / Math.pow(10, 2));
    if (invoiceNumber == null) {
        invoiceNumber = "";
    }
    if (type == 1) {
        printer1Init = printer1.StartBon(1, 1, "", invoiceNumber, invoiceTaxNumber, invoiceVatNumber, companyName, city, address, "MOL", invoiceReceiver);
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("             [RE-PRINT]", "BOLD", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("Cashier       : " + cashierName, "", 1);
        }
        if (printer1Init == 0 && accountNumber != null) {
            printer1Init = printer1.AddLine("Account #     : " + accountNumber, "", 1);
        }
        if (printer1Init == 0 && secDepAccountNumber != null) {
            printer1Init = printer1.AddLine("Account #     : " + secDepAccountNumber, "", 1);
        }
        if (printer1Init == 0 && accountName != null) {
            printer1Init = printer1.AddLine("Account Name  : " + accountName, "", 1);
        }
        if (printer1Init == 0 && customerVATNumber != null) {
            printer1Init = printer1.AddLine("Customer VAT Number  : " + customerVATNumber, "", 1);
        }
        if (printer1Init == 0 && secDepAccountName != null) {
            printer1Init = printer1.AddLine("Account Name  : " + secDepAccountName, "", 1);
        }
        if (printer1Init == 0 && comment != null) {
            printer1Init = printer1.AddLine("REF           : " + comment, "", 1);
        }
        if (printer1Init == 0 && invoiceNumber != null && invoiceNumber != "") {
            printer1Init = printer1.AddLine("Invoice #     : " + invoiceNumber, "", 1);
        }
        if (printer1Init == 0 && refInvoiceNumber != null) {
            printer1Init = printer1.AddLine("Ref Invoice # : " + refInvoiceNumber, "", 1);
        }
        if (printer1Init == 0 && receiptNumber != null) {
            printer1Init = printer1.AddLine("Receipt Number: " + receiptNumber, "", 1);
        }
        if (printer1Init == 0 && transactionType != null) {
            printer1Init = printer1.AddLine("Description   : " + transactionType, "", 1);
        }
        if (printer1Init == 0 && mobileNumber != null) {
            printer1Init = printer1.AddLine("Mobile Number : " + mobileNumber, "", 1);
        }
        if (printer1Init == 0 && documentNumber != null) {
            printer1Init = printer1.AddLine("Document #    : " + documentNumber, "", 1);
        }
        if (printer1Init == 0 && tenderedCurrency != null) {
            printer1Init = printer1.AddLine("Tendered Currency    : " + tenderedCurrency, "", 1);
        }
        if (printer1Init == 0 && depositSerialNumber != null) {
            printer1Init = printer1.AddLine("Deposit Serial #   : " + depositSerialNumber, "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
        }
        if (amountDue != null && amountDue != "") {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("Invoice Total :               " + currency + amountDue + "", "", 1);
            }
        }
        if (amountTendered != null && amountTendered != "") {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("AMOUNT TENDERED :              " + currency + amountTendered + "", "", 1);
            }
        }
        if (change != null && change != "") {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("CHANGE :                     " + changeCurrency + "" + change + "", "BOLD", 1);
            }
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
        }

        for (let i = 0; i < paymentTypeObject.length; i++) {
            let object = paymentTypeObject[i];
            if (object["paymentTypeString"] == "POS") {
                object["paymentTypeString"] = "DEBIT CARD";
            }
            if (object["paymentTypeString"] == "DEBIT CARD") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "             " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["posRefNumber"] != null && object["posRefNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (POS Ref Number: " + object["posRefNumber"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
            else if (object["paymentTypeString"] == "CHEQUE") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "                 " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["chequeType"] != null && object["chequeType"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (Cheque Type   : " + object["chequeType"] + ")", "", 1);
                    }
                }
                if (object["chequeNumber"] != null && object["chequeNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (Cheque Number : " + object["chequeNumber"] + ")", "", 1);
                    }
                }
                if (object["bank"] != null && object["bank"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (Bank: " + object["bank"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
            else if (object["paymentTypeString"] == "CASH") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                }
            }
            else if (object["paymentTypeString"] == "CASH-RTGS") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                }
            }
            else if (object["paymentTypeString"] == "RTGS") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["rtgsRefNumber"] != null && object["rtgsRefNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (RTGS Ref Number: " + object["rtgsRefNumber"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
            else if (object["paymentTypeString"] == "ECOCASH") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "                 " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["ecocashRefNumber"] != null && object["ecocashRefNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (ECOCASH Ref Number: " + object["ecocashRefNumber"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
        }

        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(signature, "", 1);
        }
        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" PLEASE RETAIN AS PROOF OF PAYMENT ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) printer1Init = printer1.EndBon();
        printer1.Close();
    }
    else if (type == 2) {
        console.log("in side type two");
        printer1Init = printer1.StartBon(1, 1, "", invoiceNumber, invoiceTaxNumber, invoiceVatNumber, companyName, city, address, "MOL", invoiceReceiver);
        if (isReturn == 1) {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("          [RETURN RE-PRINT]", "BOLD", 1);
            }
        }
        else {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("             [RE-PRINT]", "BOLD", 1);
            }
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("Cashier       : " + cashierName, "", 1);
        }
        if (printer1Init == 0 && accountNumber != null) {
            printer1Init = printer1.AddLine("Account #     : " + accountNumber, "", 1);
        }
        if (printer1Init == 0 && secDepAccountNumber != null) {
            printer1Init = printer1.AddLine("Account #     : " + secDepAccountNumber, "", 1);
        }
        if (printer1Init == 0 && accountName != null) {
            printer1Init = printer1.AddLine("Account Name  : " + accountName, "", 1);
        }
        if (printer1Init == 0 && customerVATNumber != null) {
            printer1Init = printer1.AddLine("Customer VAT Number  : " + customerVATNumber, "", 1);
        }
        if (printer1Init == 0 && secDepAccountName != null) {
            printer1Init = printer1.AddLine("Account Name  : " + secDepAccountName, "", 1);
        }
        if (printer1Init == 0 && comment != null) {
            printer1Init = printer1.AddLine("REF           : " + comment, "", 1);
        }
        if (printer1Init == 0 && invoiceNumber != null && invoiceNumber != "") {
            printer1Init = printer1.AddLine("Invoice #     : " + invoiceNumber, "", 1);
        }
        if (printer1Init == 0 && refInvoiceNumber != null) {
            printer1Init = printer1.AddLine("Ref Invoice # : " + refInvoiceNumber, "", 1);
        }
        if (printer1Init == 0 && receiptNumber != null) {
            printer1Init = printer1.AddLine("Receipt Number: " + receiptNumber, "", 1);
        }
        if (printer1Init == 0 && transactionType != null) {
            printer1Init = printer1.AddLine("Description   : " + transactionType, "", 1);
        }
        if (printer1Init == 0 && mobileNumber != null) {
            printer1Init = printer1.AddLine("Mobile Number : " + mobileNumber, "", 1);
        }
        if (printer1Init == 0 && documentNumber != null) {
            printer1Init = printer1.AddLine("Document #    : " + documentNumber, "", 1);
        }
        if (printer1Init == 0 && tenderedCurrency != null) {
            printer1Init = printer1.AddLine("Tendered Currency    : " + tenderedCurrency, "", 1);
        }
        if (printer1Init == 0 && depositSerialNumber != null) {
            printer1Init = printer1.AddLine("Deposit Serial #   : " + depositSerialNumber, "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("------------------------------------ ", "", 1);
        }

        if (jsonObject != null || jsonObject != undefined) {
            for (let i = 0; i < jsonObject.length; i++) {
                let object = jsonObject[i];

                if (object["paymentItemType"] == "Free Product" || object["paymentItemType"] == "Credit Sale") {
                    isFoc = 1;
                    if (object["description"] == "Discount Allowed") {

                    }
                    else {
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(object["description"], "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("   " + object["quantity"] + " X      " + object["unitPrice"] + "          $" + object["subTotal"], "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("DISCOUNT" + "     -" + 100 + ".00%      " + "$" + "-" + object["subTotal"], "", 1);
                        }
                    }

                }
                else {

                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["description"], "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("  " + object["quantity"] + " X      " + object["unitPrice"] + "       " + tenderedCurrency + " " + object["subTotal"], "", 1);
                    }

                }
            }
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("----------------------------------", "", 1);
        }

        if (isFoc == 1) {
            let amount = 0.00;
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("TAXBL1" + "                       " + tenderedCurrency + amount, "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("TAX1 14.50%" + "                  " + tenderedCurrency + amount, "", 1);
            }

            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("----------------------------------", "", 1);
            }

            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("TOTAL" + "                        " + tenderedCurrency + amount, "", 1);
            }

            for (let i = 0; i < paymentTypeObject.length; i++) {
                let object = paymentTypeObject[i];

                if (object["paymentTypeString"] == "POS") {
                    object["paymentTypeString"] = "DEBIT CARD";
                }
                if (object["paymentTypeString"] == "DEBIT CARD") {

                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + tenderedCurrency + object["amount"], "", 1);
                    }
                } else if (object["paymentTypeString"] == "CHEQUE") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + tenderedCurrency + object["amount"], "", 1);
                    }
                } else {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(object["paymentTypeString"] + "                         " + tenderedCurrency + object["amount"], "", 1);
                    }
                }


            }
            if (change != null) {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("CHANGE" + "                    " + changeCurrency + "" + change, "BOLD", 1);
                }
            }
        }
        else {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("TAXBL1" + "                     " + tenderedCurrency + total, "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("TAX1 14.50%" + "                " + tenderedCurrency + tax, "", 1);
            }

            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("--------------------------------", "", 1);
            }

            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("TOTAL" + "                      " + tenderedCurrency + total, "", 1);
            }

            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("TOTAL TENDERED" + "              " + tenderedCurrency + amountTendered, "", 1);
            }

            if (change != null) {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("CHANGE" + "                      " + changeCurrency + "" + change, "BOLD", 1);
                }
            }
        }

        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("ITEM #" + "                      " + itemNumber, "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("--------------------------------", "", 1);
        }
        for (let i = 0; i < paymentTypeObject.length; i++) {
            let object = paymentTypeObject[i];
            if (object["paymentTypeString"] == "POS") {
                object["paymentTypeString"] = "DEBIT CARD";
            }

            if (object["paymentTypeString"] == "DEBIT CARD") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("DEBIT CARD" + "               " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["posRefNumber"] != null && object["posRefNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (POS Ref Number: " + object["posRefNumber"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
            else if (object["paymentTypeString"] == "CHEQUE") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("CHEQUE" + "                   " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["chequeType"] != null && object["chequeType"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (Cheque Type   : " + object["chequeType"] + ")", "", 1);
                    }
                }
                if (object["chequeNumber"] != null && object["chequeNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (Cheque Number : " + object["chequeNumber"] + ")", "", 1);
                    }
                }
                if (object["bank"] != null && object["bank"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (Bank: " + object["bank"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
            else if (object["paymentTypeString"] == "CASH") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("CASH" + "                     " + object["currency"] + " " + object["amount"], "", 1);
                }
            }
            else if (object["paymentTypeString"] == "CASH-RTGS") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("CASH-RTGS" + "                     " + object["currency"] + " " + object["amount"], "", 1);
                }
            }
            else if (object["paymentTypeString"] == "RTGS") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("RTGS" + "                       " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["rtgsRefNumber"] != null && object["rtgsRefNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (RTGS Ref Number: " + object["rtgsRefNumber"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
            else if (object["paymentTypeString"] == "ECOCASH") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("ECOCASH" + "                   " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["ecocashRefNumber"] != null && object["ecocashRefNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (ECOCASH Ref Number: " + object["ecocashRefNumber"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }

        }

        if (isSeriallyTracked == "yes" && isBulkPurchase != 1) {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("----------------------------------", "", 1);
            }
            for (let i = 0; i < serialObject.length; i++) {
                let object = serialObject[i];
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("  ", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("Description: " + object["stockItemCode"], "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("Serial Number: " + object["serialNumber"], "", 1);
                }
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("----------------------------------", "", 1);
            }
        }

        if (isBulkPurchase == 1) {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("  ", "", 1);
            }
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("                Serials", "", 1);
            }
            for (let i = 0; i < serialObject.length; i++) {
                let object = serialObject[i];
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["stockItemCode"] + ": " + object["lowerLimit"] + "-" + object["upperLimit"], "", 1);
                }
            }
        }

        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(signature, "", 1);
        }
        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" PLEASE RETAIN AS PROOF OF PAYMENT ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) printer1Init = printer1.EndBon();
        printer1.Close();
        exeededLimit = "true";

        if (isRechargeCard == 1) {
            updatedRechargeLength = rechargeCardObject.length;
            while (exeededLimit == "true") {

                let printer1 = new ActiveXObject("EltradeFPAx.EltradeFprn");
                printer1Init = printer1.Init(comport, 115200, 0, 3, 0, "");

                printer1Init = printer1.StartBon(1, 1, "", invoiceNumber, invoiceTaxNumber, invoiceVatNumber, companyName, city, address, "MOL", invoiceReceiver);
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(" ", "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("Cashier       : " + cashierName, "", 1);
                }
                if (printer1Init == 0 && accountNumber != null) {
                    printer1Init = printer1.AddLine("Account #     : " + accountNumber, "", 1);
                }
                if (printer1Init == 0 && secDepAccountNumber != null) {
                    printer1Init = printer1.AddLine("Account #     : " + secDepAccountNumber, "", 1);
                }
                if (printer1Init == 0 && accountName != null) {
                    printer1Init = printer1.AddLine("Account Name  : " + accountName, "", 1);
                }
                if (printer1Init == 0 && customerVATNumber != null) {
                    printer1Init = printer1.AddLine("Customer VAT Number  : " + customerVATNumber, "", 1);
                }
                if (printer1Init == 0 && secDepAccountName != null) {
                    printer1Init = printer1.AddLine("Account Name  : " + secDepAccountName, "", 1);
                }
                if (printer1Init == 0 && comment != null) {
                    printer1Init = printer1.AddLine("REF           : " + comment, "", 1);
                }
                if (printer1Init == 0 && invoiceNumber != null && invoiceNumber != "") {
                    printer1Init = printer1.AddLine("Invoice #     : " + invoiceNumber, "", 1);
                }
                if (printer1Init == 0 && refInvoiceNumber != null) {
                    printer1Init = printer1.AddLine("Ref Invoice # : " + refInvoiceNumber, "", 1);
                }
                if (printer1Init == 0 && receiptNumber != null) {
                    printer1Init = printer1.AddLine("Receipt Number: " + receiptNumber, "", 1);
                }
                if (printer1Init == 0 && transactionType != null) {
                    printer1Init = printer1.AddLine("Description   : " + transactionType, "", 1);
                }
                if (printer1Init == 0 && mobileNumber != null) {
                    printer1Init = printer1.AddLine("Mobile Number : " + mobileNumber, "", 1);
                }
                if (printer1Init == 0 && documentNumber != null) {
                    printer1Init = printer1.AddLine("Document #    : " + documentNumber, "", 1);
                }
                if (printer1Init == 0 && tenderedCurrency != null) {
                    printer1Init = printer1.AddLine("Tendered Currency    : " + tenderedCurrency, "", 1);
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("------------------------------------ ", "", 1);
                }

                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("", "", 1);
                }

                console.log("Done adding cashier header for recharge cards : " + loop)

                for (let i = 0; i < loop; i++) {
                    if (rechargeCardObject[i] != null) {

                        let object = rechargeCardObject[i];
                        //if (printer1Init==0){ printer1Init = printer1.AddLine("Package: "+object["cardPackage"],"", 1);}
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("Card Key: ", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(object["cardKey"], "BOLD", 1);
                        }

                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("Expiry Date: " + expiryDateObject.expiryDate, "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("S/N: " + object["serialNumber"], "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("Batch Id: " + object["batchNumber"], "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine("Denomination: " + "$" + object["denomination"], "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(object["rechargeInstruction"] + " Dial 111 for Customer Care or sms help to 111", "", 1);
                        }
                        if (printer1Init == 0) {
                            printer1Init = printer1.AddLine(" ", "", 1);
                        }
                    }
                    rechargeCardObject[i] = null;

                }
                loop = loop + 30;
                updatedRechargeLength = updatedRechargeLength - 30;
                if (updatedRechargeLength == 0 || updatedRechargeLength < 0) {
                    exeededLimit = "false";
                }
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine("----------------------------------", "", 1);
                }

                if (printer1Init == 0) printer1Init = printer1.EndBon();
                printer1.Close();
            }

        }

    }
    else if (type == 3) {
        amountDue = paymentJsObject.amountDue + "0";
        printer1Init = printer1.StartBon(1, 1, "", invoiceNumber, invoiceTaxNumber, invoiceVatNumber, companyName, city, address, "MOL", invoiceReceiver);
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("Cashier       : " + batchJsObject.cashier, "", 1);
        }
        if (printer1Init == 0 && transactionType != null) {
            printer1Init = printer1.AddLine("Description   : " + batchJsObject.transactionType, "", 1);
        }
        if (printer1Init == 0 && documentNumber != null) {
            printer1Init = printer1.AddLine("Document #    : " + batchJsObject.batchDocumentNumber, "", 1);
        }
        if (printer1Init == 0 && tenderedCurrency != null) {
            printer1Init = printer1.AddLine("Tendered Currency    : " + tenderedCurrency, "", 1);
        }
        if (printer1Init == 0 && depositSerialNumber != null) {
            printer1Init = printer1.AddLine("Payments Count #   : " + batchJsObject.batchCount, "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
        }
        if (batchJsObject.batchTotalAmount != null && batchJsObject.batchTotalAmount != "") {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("Invoice Total :            " + tenderedCurrency + amountDue + "", "", 1);
            }
        }
        if (batchJsObject.tenderedAmount != null && batchJsObject.tenderedAmount != "") {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("AMOUNT TENDERED :           " + tenderedCurrency + batchJsObject.tenderedAmount + "", "", 1);
            }
        }

        change = batchJsObject.tenderedAmount - batchJsObject.batchTotalAmount;

        if (change != null && change != "" && change > 0) {
            if (printer1Init == 0) {
                printer1Init = printer1.AddLine("CHANGE :                  " + changeCurrency + change + "", "", 1);
            }
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
        }

        for (let i = 0; i < paymentTypeObject.length; i++) {
            let object = paymentTypeObject[i];
            if (object["paymentTypeString"] == "POS") {
                object["paymentTypeString"] = "DEBIT CARD";
            }
            if (object["paymentTypeString"] == "DEBIT CARD") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "             " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["posRefNumber"] != null && object["posRefNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (POS Ref Number: " + object["posRefNumber"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
            else if (object["paymentTypeString"] == "CHEQUE") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "                 " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["chequeType"] != null && object["chequeType"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (Cheque Type   : " + object["chequeType"] + ")", "", 1);
                    }

                }
                if (object["chequeNumber"] != null && object["chequeNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (Cheque Number : " + object["chequeNumber"] + ")", "", 1);
                    }
                }
                if (object["bank"] != null && object["bank"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (Bank: " + object["bank"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
            else if (object["paymentTypeString"] == "CASH") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                }
            }
            else if (object["paymentTypeString"] == "RTGS") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "                   " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["rtgsRefNumber"] != null && object["rtgsRefNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (RTGS Ref Number: " + object["rtgsRefNumber"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
            else if (object["paymentTypeString"] == "ECOCASH") {
                if (printer1Init == 0) {
                    printer1Init = printer1.AddLine(object["paymentTypeString"] + "                 " + object["currency"] + " " + object["amount"], "", 1);
                }
                if (object["ecocashRefNumber"] != null && object["ecocashRefNumber"] != "") {
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine("      (ECOCASH Ref Number: " + object["ecocashRefNumber"] + ")", "", 1);
                    }
                    if (printer1Init == 0) {
                        printer1Init = printer1.AddLine(" ", "", 1);
                    }
                }
            }
        }

        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(signature, "", 1);
        }
        if (printer1Init == 0 && signature != null) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine("---------------------------------- ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" PLEASE RETAIN AS PROOF OF PAYMENT ", "", 1);
        }
        if (printer1Init == 0) {
            printer1Init = printer1.AddLine(" ", "", 1);
        }
        if (printer1Init == 0) printer1Init = printer1.EndBon();
        printer1.Close();
    }
    else {
        alert(printer1.GetLastError(1));
    }
}

function printZReport(value) {
    try {
        let result1 = document.getElementById("result");

        let comport = 3;

        let printer1 = new ActiveXObject("EltradeFPAx.EltradeFprn");
        let printer1Init = printer1.Init(comport, 115200, 0, 3, 0, "");
        printer1Init = printer1.RunZReport();

        if (printer1Init > 0) {
            result1.style.color = "red";
            result1.innerHTML = "an error occured, check printer connection";
        }
        else {
            result1.style.color = "green";
            result1.innerHTML = "Z-Report printed successfully";
        }
        printer1.Close();
    } catch (e) {

        if(value == 1){
            printRevmaxZReport(function (response) {
                if (response.Response.Code == "1") {
                    loadZReport(response.Response.Data.ZREPORT);
                } else {
                    console.log(">>>>>>> Error : " + response.Response.Message);
                    alert("Failed to fetch Z Reports");
                }
            });
        }else if(value == 2) {

            printFssZReport(function (response) {
                if (response !== null) {
                    console.log("User reports response: " + response.userName);
                    loadFssZReport(response);
                } else {
                    console.log(">>>>>>> Error : " + response);
                    alert("Failed to fetch Z Reports");
                }
            });
        }else{

        }

    }
}

function printRevmaxZReport(callback) {
    let xhr = new XMLHttpRequest();
    xhr.open("GET", "http://revmax:10000/api/RevmaxAPI/ZReport", false);
    xhr.onreadystatechange = function () {
        if (this.readyState != 4) return;
        if (this.status == 200) {
            callback(JSON.parse(this.responseText));
        }
    };
    xhr.send();
    // callback(loadDummyZReport());
}

function printFssZReport(callback) {
    let xhr = new XMLHttpRequest();
    let token = document.getElementById('token').value;
    let userName = document.getElementById('userName').value;
    let startDate = document.getElementById("reportDate").value;

    console.log("Username is :" + userName);
    xhr.open("GET", "http://localhost:9090/api/reports/users/"+ userName + "?startDate=" + startDate, false);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.setRequestHeader('Authorization', token);
    xhr.onreadystatechange = function () {
        if (this.readyState != 4) return;
        if (this.status == 200) {
            callback(JSON.parse(this.responseText));
            console.log(JSON.parse(this.responseText));
        }
    };
    xhr.send();
    // callback(loadDummyZReport());
}

function loadZReport(zreport){
    let bpn = null;
    if(Array.isArray(zreport)){
        bpn = zreport[0].BPNUM;
    } else {
        bpn = zreport.BPNUM;
    }
    let zReportDiv = document.createElement("div");
    let headerInfo = document.createElement("div");
    let zreportHeading = document.createElement("div");
    let logo = document.createElement("IMG");
    logo.setAttribute("src", "/resources/images/zimtrade_logo.png");
    logo.setAttribute("width", "250");
    logo.setAttribute("height", "120");
    headerInfo.innerHTML = "" +
        "<center>" +
            "<table>" +
                "<thead>" +
                    "<tr>" +
                        "<td><b>BPN :"+ bpn  +"</b></td>" +
                    "</tr> " +
                    "<tr> " +
                        "<td >Econet Wireless</td>" +
                    "</tr> " +
                    "<tr>" +
                        "<td>2 Old Mutare Rd</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td>Msasa, Harare</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td>Telephone: 486121/9</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td>Email : sales@econet.co.zw</td>" +
                    "</tr>" +
                "</thead>" +
            "</table>" +
            "<p>-------------------------------</p>" +
        "</center>";
    zreportHeading.innerHTML = "" +
        "<center><b>SESSION Z REPORT</b><br>" +
        "<p>-------------------------------</p></center>";
    // zReportDiv.appendChild(logo);
    zReportDiv.appendChild(headerInfo);
    zReportDiv.appendChild(zreportHeading);
    if(Array.isArray(zreport)){
        for(let i = 0; i < zreport.length; i++){
            zReportDiv.appendChild(populateZReport(zreport[i]));
        }
    } else {
        zReportDiv.appendChild(populateZReport(zreport));
    }
    printZReportContent(zReportDiv);
}



function loadFssZReport(zreport){
    let bpn = 200002652;
    // if(Array.isArray(zreport)){
    //     bpn = zreport[0].BPNUM;
    // } else {
    //     bpn = zreport.BPNUM;
    // }
    let zReportDiv = document.createElement("div");
    let headerInfo = document.createElement("div");
    let zreportHeading = document.createElement("div");
    let logo = document.createElement("IMG");
    logo.setAttribute("src", "/resources/images/zimtrade_logo.png");
    logo.setAttribute("width", "250");
    logo.setAttribute("height", "120");
    headerInfo.innerHTML = "" +
        "<center>" +
        "<table>" +
        "<thead>" +
        "<tr>" +
        "<td><b>BPN :"+ bpn  +"</b></td>" +
        "</tr> " +
        "<tr> " +
        "<td >Econet Wireless</td>" +
        "</tr> " +
        "<tr>" +
        "<td>2 Old Mutare Rd</td>" +
        "</tr>" +
        "<tr>" +
        "<td>Msasa, Harare</td>" +
        "</tr>" +
        "<tr>" +
        "<td>Telephone: 486121/9</td>" +
        "</tr>" +
        "<tr>" +
        "<td>Email : sales@econet.co.zw</td>" +
        "</tr>" +
        "</thead>" +
        "</table>" +
        "<p>-------------------------------</p>" +
        "</center>";
    zreportHeading.innerHTML = "" +
        "<center><b>SESSION Z REPORT</b><br>" +
        "<p>-------------------------------</p></center>";

    zReportDiv.appendChild(headerInfo);
    zReportDiv.appendChild(zreportHeading);
    if(Array.isArray(zreport)){
        for(let i = 0; i < zreport.length; i++){
            zReportDiv.appendChild(populateFssZReport(zreport[i]));
        }
    } else {
        zReportDiv.appendChild(populateFssZReport(zreport));
    }
    printZReportContent(zReportDiv);
}



function populateZReport(zreport){
    let zreportTable = document.createElement("table");

    let dateTimeTR = document.createElement("tr");
    let date = document.createElement("td");
    let time = document.createElement("td");
    date.innerHTML = zreport.DATE;
    time.innerHTML = zreport.TIME;
    dateTimeTR.appendChild(date);
    dateTimeTR.appendChild(time);
    zreportTable.appendChild(dateTimeTR);

    let currencyTR = document.createElement("tr");
    let currencyName = document.createElement("td");
    let currencyValue = document.createElement("td");
    currencyName.innerHTML = "CURRENCY :";
    currencyValue.innerHTML = zreport.CURRENCY;
    currencyTR.appendChild(currencyName);
    currencyTR.appendChild(currencyValue);
    zreportTable.appendChild(currencyTR);

    let dividerTR1 = document.createElement("tr");
    dividerTR1.innerHTML = "<td colspan='2'><center><p>-------------------------------</p></center></td>";
    zreportTable.appendChild(dividerTR1);

    let totalsTR = document.createElement("tr");
    let totalsName = document.createElement("td");
    totalsName.innerHTML = "<b>TOTALS :</b>";
    totalsTR.appendChild(totalsName);
    zreportTable.appendChild(totalsTR);

    let dailyTotalAmtTR = document.createElement("tr");
    let dailyTotalAmtName = document.createElement("td");
    let dailyTotalAmtValue = document.createElement("td");
    dailyTotalAmtName.innerHTML = "DAILYTOTALAMOUNT :";
    dailyTotalAmtValue.innerHTML = (parseFloat(zreport.TOTALS.DAILYTOTALAMOUNT)/100).toFixed(2);
    dailyTotalAmtTR.appendChild(dailyTotalAmtName);
    dailyTotalAmtTR.appendChild(dailyTotalAmtValue);
    zreportTable.appendChild(dailyTotalAmtTR);

    let grossTR = document.createElement("tr");
    let grossName = document.createElement("td");
    let grossValue = document.createElement("td");
    grossName.innerHTML = "GROSS :";
    grossValue.innerHTML = (parseFloat(zreport.TOTALS.GROSS)/100).toFixed(2);
    grossTR.appendChild(grossName);
    grossTR.appendChild(grossValue);
    zreportTable.appendChild(grossTR);

    let correctionsTR = document.createElement("tr");
    let correctionsName = document.createElement("td");
    let correctionsValue = document.createElement("td");
    correctionsName.innerHTML = "CORRECTIONS :";
    correctionsValue.innerHTML = (parseFloat(zreport.TOTALS.CORRECTIONS)/100).toFixed(2);
    correctionsTR.appendChild(correctionsName);
    correctionsTR.appendChild(correctionsValue);
    zreportTable.appendChild(correctionsTR);

    let discountsTR = document.createElement("tr");
    let discountsName = document.createElement("td");
    let discountsValue = document.createElement("td");
    discountsName.innerHTML = "DISCOUNTS :";
    discountsValue.innerHTML = (parseFloat(zreport.TOTALS.DISCOUNTS)/100).toFixed(2);
    discountsTR.appendChild(discountsName);
    discountsTR.appendChild(discountsValue);
    zreportTable.appendChild(discountsTR);

    let surchargesTR = document.createElement("tr");
    let surchargesName = document.createElement("td");
    let surchargesValue = document.createElement("td");
    surchargesName.innerHTML = "SURCHARGES :";
    surchargesValue.innerHTML = (parseFloat(zreport.TOTALS.SURCHARGES)/100).toFixed(2);
    surchargesTR.appendChild(surchargesName);
    surchargesTR.appendChild(surchargesValue);
    zreportTable.appendChild(surchargesTR);

    let ticketsVoidTR = document.createElement("tr");
    let ticketsVoidName = document.createElement("td");
    let ticketsVoidValue = document.createElement("td");
    ticketsVoidName.innerHTML = "TICKETSVOID :";
    ticketsVoidValue.innerHTML = zreport.TOTALS.TICKETSVOID;
    ticketsVoidTR.appendChild(ticketsVoidName);
    ticketsVoidTR.appendChild(ticketsVoidValue);
    zreportTable.appendChild(ticketsVoidTR);

    let ticketsVoidTotalTR = document.createElement("tr");
    let ticketsVoidTotalName = document.createElement("td");
    let ticketsVoidTotalValue = document.createElement("td");
    ticketsVoidTotalName.innerHTML = "TICKETSVOIDTOTAL :";
    ticketsVoidTotalValue.innerHTML = zreport.TOTALS.TICKETSVOIDTOTAL;
    ticketsVoidTotalTR.appendChild(ticketsVoidTotalName);
    ticketsVoidTotalTR.appendChild(ticketsVoidTotalValue);
    zreportTable.appendChild(ticketsVoidTotalTR);

    let ticketsFiscalTR = document.createElement("tr");
    let ticketsFiscalName = document.createElement("td");
    let ticketsFiscalValue = document.createElement("td");
    ticketsFiscalName.innerHTML = "TICKETSFISCAL :";
    ticketsFiscalValue.innerHTML = zreport.TOTALS.TICKETSFISCAL;
    ticketsFiscalTR.appendChild(ticketsFiscalName);
    ticketsFiscalTR.appendChild(ticketsFiscalValue);
    zreportTable.appendChild(ticketsFiscalTR);

    let ticketsNonFiscalTR = document.createElement("tr");
    let ticketsNonFiscalName = document.createElement("td");
    let ticketsNonFiscalValue = document.createElement("td");
    ticketsNonFiscalName.innerHTML = "TICKETSNONFISCAL :";
    ticketsNonFiscalValue.innerHTML = zreport.TOTALS.TICKETSNONFISCAL;
    ticketsNonFiscalTR.appendChild(ticketsNonFiscalName);
    ticketsNonFiscalTR.appendChild(ticketsNonFiscalValue);
    zreportTable.appendChild(ticketsNonFiscalTR);

    let dividerTR2 = document.createElement("tr");
    dividerTR2.innerHTML = "<td colspan='2'><center><p>-------------------------------</p></center></td>";
    zreportTable.appendChild(dividerTR2);

    let vatTotalsTR = document.createElement("tr");
    let vatTotalsName = document.createElement("td");
    vatTotalsName.innerHTML = "<b>VATTOTALS :</b>";
    vatTotalsTR.appendChild(vatTotalsName);
    zreportTable.appendChild(vatTotalsTR);

    for(let i = 0; i < zreport.VATTOTALS.length; i++){
        let vatRateTR = document.createElement("tr");
        let vatRateName = document.createElement("td");
        let vatRateValue = document.createElement("td");
        vatRateName.innerHTML = "VATRATE :";
        vatRateValue.innerHTML = zreport.VATTOTALS[i].VATRATE;
        vatRateTR.appendChild(vatRateName);
        vatRateTR.appendChild(vatRateValue);
        zreportTable.appendChild(vatRateTR);

        let netAmountTR = document.createElement("tr");
        let netAmountName = document.createElement("td");
        let netAmountValue = document.createElement("td");
        netAmountName.innerHTML = "NETTAMOUNT :";
        netAmountValue.innerHTML = (parseFloat(zreport.VATTOTALS[i].NETTAMOUNT)/100).toFixed(2);
        netAmountTR.appendChild(netAmountName);
        netAmountTR.appendChild(netAmountValue);
        zreportTable.appendChild(netAmountTR);

        let taxAmountTR = document.createElement("tr");
        let taxAmountName = document.createElement("td");
        let taxAmountValue = document.createElement("td");
        taxAmountName.innerHTML = "TAXAMOUNT :";
        taxAmountValue.innerHTML = (parseFloat(zreport.VATTOTALS[i].TAXAMOUNT)/100).toFixed(2);
        taxAmountTR.appendChild(taxAmountName);
        taxAmountTR.appendChild(taxAmountValue);
        zreportTable.appendChild(taxAmountTR);

        let dividerTR = document.createElement("tr");
        dividerTR.innerHTML = "<td colspan='2'></td>";
        zreportTable.appendChild(dividerTR);

    }

    let signatureTR = document.createElement("tr");
    let signatureValue = document.createElement("td");
    signatureValue.innerHTML = zreport.Signature;
    signatureTR.appendChild(signatureValue);
    zreportTable.appendChild(signatureTR);

    let dividerTR = document.createElement("tr");
    dividerTR.innerHTML = "<td colspan='2'><center><p>-------------------------------</p></center></td>";
    zreportTable.appendChild(dividerTR);

    return zreportTable;
}



function populateFssZReport(zreport){
    let zreportTable = document.createElement("table");
    console.log("Populate Z report with :" + zreport.currencyReports.length);
    let dateTimeTR = document.createElement("tr");
    zreportTable.style.width = "100%";
    let date = document.createElement("td");

    let time = document.createElement("td");
    date.innerHTML = zreport.reportStartDate;
    time.innerHTML = '00-00-00';
    dateTimeTR.appendChild(date);
    dateTimeTR.appendChild(time);
    zreportTable.appendChild(dateTimeTR);

    for(let i = 0; i < zreport.currencyReports.length; i++){
       console.log("inside loop :" + i);
        let currencyTR = document.createElement("tr");
        let currencyName = document.createElement("td");
        let currencyValue = document.createElement("td");
        currencyName.innerHTML = "CURRENCY :";
        currencyValue.innerHTML = zreport.currencyReports[i].name;
        currencyTR.appendChild(currencyName);
        currencyTR.appendChild(currencyValue);
        zreportTable.appendChild(currencyTR);

        let dailyTotalAmtTR = document.createElement("tr");
        let dailyTotalAmtName = document.createElement("td");
        let dailyTotalAmtValue = document.createElement("td");
        dailyTotalAmtName.innerHTML = "DAILYTOTALAMOUNT :";
        dailyTotalAmtValue.innerHTML = (parseFloat(zreport.currencyReports[i].invoiceAmountTotal)).toFixed(2);
        dailyTotalAmtTR.appendChild(dailyTotalAmtName);
        dailyTotalAmtTR.appendChild(dailyTotalAmtValue);
        zreportTable.appendChild(dailyTotalAmtTR);

        let vatRateTR = document.createElement("tr");
        let vatRateName = document.createElement("td");
        let vatRateValue = document.createElement("td");
        vatRateName.innerHTML = "VATRATE :";
        vatRateValue.innerHTML = 15 + '%';
        vatRateTR.appendChild(vatRateName);
        vatRateTR.appendChild(vatRateValue);
        zreportTable.appendChild(vatRateTR);

        let taxAmountTR = document.createElement("tr");
        let taxAmountName = document.createElement("td");
        let taxAmountValue = document.createElement("td");
        taxAmountName.innerHTML = "TAXAMOUNT :";
        taxAmountValue.innerHTML = (parseFloat(zreport.currencyReports[i].invoiceTaxAmountTotal)/100).toFixed(2);
        taxAmountTR.appendChild(taxAmountName);
        taxAmountTR.appendChild(taxAmountValue);
        zreportTable.appendChild(taxAmountTR);

        let dividerTR = document.createElement("tr");
        dividerTR.innerHTML = "<td colspan='2'><center><p>-------------------------------</p></center></td>";
        zreportTable.appendChild(dividerTR);

    }

    let dividerTR1 = document.createElement("tr");
    dividerTR1.innerHTML = "<td colspan='2'><center><p>-------------------------------</p></center></td>";
    zreportTable.appendChild(dividerTR1);

    let totalsTR = document.createElement("tr");
    let totalsName = document.createElement("td");
    let totalsValue = document.createElement("td");
    totalsName.innerHTML = "<b>TOTAL INVOICES :</b>";
    totalsValue.innerHTML = zreport.numberOfInvoices;
    totalsTR.appendChild(totalsName);
    totalsTR.appendChild(totalsValue);
    zreportTable.appendChild(totalsTR);


    let dividerTR = document.createElement("tr");
    dividerTR.innerHTML = "<td colspan='2'><center><p>-------------------------------</p></center></td>";
    zreportTable.appendChild(dividerTR);

    return zreportTable;
}


function printZReportContent(theContainer) {
    let newWindowObject = window.open('', "ZReport",
        "width=520,height=625,top=50,left=50,toolbars=no,scrollbars=no,status=no,resizable=no");

    newWindowObject.document.write(theContainer.innerHTML);

    newWindowObject.document.close();
    newWindowObject.focus();
    newWindowObject.print();
    newWindowObject.close();
}

function loadDummyZReport() {
        return {
            "Response": {
                "Code": "1",
                "Message": "Success",
                "Data": {
                    "ZREPORT": {
                        "Signature": "E44C85030148D3-2A-DB-B6-8C-6E-38-89-31-3C-44-E0-82-4B-DD-EE-94-62-21-52-6D-63-AF-13-A6-5A-33-7A-D4-E7-65-C8",
                        "DATE": "2021-11-29",
                        "TIME": "1953",
                        "HEADER": {
                            "LINE": [
                                "Axis Solutions",
                                "14 Arundel Road",
                                "Alexandra Park",
                                "Harare"
                            ]
                        },
                        "VATNUM": "22222222",
                        "BPNUM": "111111111",
                        "TAXOFFICE": "REGION 1",
                        "Znumber": "2021-11-29",
                        "EFDSERIAL": "E44C85030148",
                        "REGISTRATIONDATE": "2020-09-29_09:25:45",
                        "USER": "USER",
                        "CURRENCY": "ZWL",
                        "TOTALS": {
                            "DAILYTOTALAMOUNT": "0",
                            "GROSS": " 0 ",
                            "CORRECTIONS": " 0 ",
                            "DISCOUNTS": " 0 ",
                            "SURCHARGES": " 0 ",
                            "TICKETSVOID": " 0 ",
                            "TICKETSVOIDTOTAL": " 0.00 ",
                            "TICKETSFISCAL": " 0 ",
                            "TICKETSNONFISCAL": " 0 "
                        },
                        "VATTOTALS": [
                            {
                                "VATRATE": "",
                                "NETTAMOUNT": "0",
                                "TAXAMOUNT": "0"
                            },
                            {
                                "VATRATE": "0",
                                "NETTAMOUNT": "0",
                                "TAXAMOUNT": "0"
                            }
                        ]
                    }
                }
            }
        }
}