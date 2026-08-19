function parseAjaxResponse(data) {
    if (typeof data === "string") {
        try {
            return JSON.parse(data);
        } catch (e) {
            return {};
        }
    }
    return data || {};
}

function getCsrfToken() {
    var cookieParts = document.cookie ? document.cookie.split(";") : [];
    for (var i = 0; i < cookieParts.length; i++) {
        var cookie = cookieParts[i].trim();
        if (cookie.indexOf("XSRF-TOKEN=") === 0) {
            return decodeURIComponent(cookie.substring("XSRF-TOKEN=".length));
        }
    }
    var csrfMeta = document.querySelector("meta[name='_csrf']");
    return csrfMeta ? csrfMeta.getAttribute("content") : null;
}

function getScoreStatusElement() {
    return document.getElementById("scoreSaveStatus");
}

function setScoreStatus(message, isError) {
    var statusElement = getScoreStatusElement();
    if (!statusElement) {
        if (message && isError) {
            alert(message);
        }
        return;
    }

    statusElement.textContent = message || "";
    statusElement.classList.remove("success");
    statusElement.classList.remove("error");
    if (message) {
        statusElement.classList.add(isError ? "error" : "success");
    }
}

function getScoreFieldStatusElement(scoreField) {
    if (!scoreField || !scoreField.id) {
        return null;
    }
    var statusElement = document.getElementById(scoreField.id + "Status");
    if (statusElement) {
        return statusElement;
    }

    statusElement = document.createElement("div");
    statusElement.id = scoreField.id + "Status";
    statusElement.className = "score-field-status";
    statusElement.setAttribute("role", "alert");
    scoreField.insertAdjacentElement("afterend", statusElement);
    return statusElement;
}

function setScoreFieldStatus(scoreField, message, isError) {
    var statusElement = getScoreFieldStatusElement(scoreField);
    if (!statusElement) {
        return;
    }

    statusElement.textContent = message || "";
    statusElement.classList.remove("error");
    scoreField.classList.remove("score-input-invalid");
    scoreField.removeAttribute("aria-invalid");
    scoreField.removeAttribute("aria-describedby");
    if (message && isError) {
        statusElement.classList.add("error");
        scoreField.classList.add("score-input-invalid");
        scoreField.setAttribute("aria-invalid", "true");
        scoreField.setAttribute("aria-describedby", statusElement.id);
    }
}

function setScoreText(elementId, value) {
    var element = document.getElementById(elementId);
    if (!element || value === undefined || value === null) {
        return;
    }
    element.textContent = value;
}

function updateOverallScores(response) {
    setScoreText("employeeOverall", response.employeeOverall);
    setScoreText("managerOverall", response.managerOverall);
    setScoreText("agreedOverall", response.agreedOverall);
    setScoreText("moderatedOverall", response.moderatedOverall);
    var moderatedInput = document.getElementById("overallScore");
    if (moderatedInput && response.moderatedOverall !== undefined && response.moderatedOverall !== null) {
        moderatedInput.value = response.moderatedOverall;
    }
}

function notifyScoreRangeError(scoreField) {
    setScoreStatus("", false);
    setScoreFieldStatus(scoreField, "Score must be between 1 and 5.", true);
    if (scoreField && typeof scoreField.focus === "function") {
        scoreField.focus();
    }
}

function isScoreOutOfRange(value) {
    if (value === null || value === undefined || String(value).trim() === "") {
        return true;
    }
    var numericValue = Number(value);
    return isNaN(numericValue) || numericValue < 1 || numericValue > 5;
}

function isScoreCaptureInput(element) {
    return element
        && element.tagName
        && element.tagName.toLowerCase() === "input"
        && /^(employeeScore|managerScore|agreedScore|overallScore)\d*$/.test(element.id || "");
}

function validateScoreCaptureInput(scoreField, focusOnError) {
    if (!scoreField) {
        return false;
    }
    if (isScoreOutOfRange(scoreField.value)) {
        setScoreStatus("", false);
        setScoreFieldStatus(scoreField, "Score must be between 1 and 5.", true);
        if (focusOnError && typeof scoreField.focus === "function") {
            scoreField.focus();
        }
        return false;
    }
    setScoreFieldStatus(scoreField, "", false);
    return true;
}

function sendScoreRequest(url, payload) {
    var csrfToken = getCsrfToken();
    if (csrfToken) {
        payload._csrf = csrfToken;
    }

    $.ajax({
        url: url,
        method: "POST",
        data: payload,
        beforeSend: function (xhr) {
            if (csrfToken) {
                xhr.setRequestHeader("X-XSRF-TOKEN", csrfToken);
            }
        },
        success: function (data) {
            var response = parseAjaxResponse(data);
            if (response.alreadyExists === true) {
                setScoreStatus("Score already exists for this target and reporting date.", true);
                return;
            }
            if (response.captureBlocked === true) {
                setScoreStatus(response.message || "Score capture is blocked for the current stage.", true);
                return;
            }
            updateOverallScores(response);
            setScoreStatus("Changes saved.", false);
        },
        error: function (xhr) {
            var response = parseAjaxResponse(xhr.responseText);
            if (response.message) {
                setScoreStatus(response.message, true);
                return;
            }
            setScoreStatus("Save failed. Please retry.", true);
        }
    });
}

function saveStandardScore(targetId) {
    var actualField = document.getElementById("actual" + targetId);
    var evidenceField = document.getElementById("evidence" + targetId);
    var justificationField = document.getElementById("justification" + targetId);
    if (!actualField || !evidenceField || !justificationField) {
        setScoreStatus("Unable to locate score fields.", true);
        return;
    }

    sendScoreRequest("/scorecards/save-standard-score", {
        targetId: targetId,
        actual: actualField.value,
        evidence: evidenceField.value,
        justification: justificationField.value
    });
}

function saveValueBasedScore(targetId, scoreFieldIdPrefix, scoreFieldName, endpoint, justificationFieldIdPrefix) {
    var scoreField = document.getElementById(scoreFieldIdPrefix + targetId);
    if (!scoreField) {
        setScoreStatus("Score field could not be found.", true);
        return false;
    }
    var scoreValue = scoreField.value;
    if (!validateScoreCaptureInput(scoreField, true)) {
        return false;
    }

    var payload = {targetId: targetId};
    payload[scoreFieldName] = scoreValue;

    if (justificationFieldIdPrefix) {
        var justificationField = document.getElementById(justificationFieldIdPrefix + targetId);
        payload.justification = justificationField ? justificationField.value : "";
    }

    sendScoreRequest(endpoint, payload);
    return true;
}

function getSelfAssessmentPipStatusElement() {
    return document.getElementById("selfAssessmentPipStatus") || getScoreStatusElement();
}

function setSelfAssessmentPipStatus(message, isError) {
    var statusElement = getSelfAssessmentPipStatusElement();
    if (!statusElement) {
        if (message && isError) {
            alert(message);
        }
        return;
    }

    statusElement.textContent = message || "";
    statusElement.classList.remove("success");
    statusElement.classList.remove("error");
    if (message) {
        statusElement.classList.add(isError ? "error" : "success");
    }
}

function getSelfAssessmentPipRowsBody() {
    return document.getElementById("selfAssessmentPipRows");
}

function getSavedSelfAssessmentPipCountField() {
    return document.getElementById("savedSelfAssessmentPipCount");
}

function updateSelfAssessmentPipCountLabel(savedCount) {
    var countLabel = document.querySelector("#selfAssessmentPipAccordion .self-assessment-pip-count");
    if (countLabel) {
        countLabel.textContent = savedCount + " saved";
    }
}

function openSelfAssessmentPipPanel() {
    var collapseElement = document.getElementById("selfAssessmentPipCollapse");
    if (!collapseElement) {
        return;
    }
    if (typeof $ !== "undefined" && typeof $(collapseElement).collapse === "function") {
        $(collapseElement).collapse("show");
        return;
    }
    collapseElement.classList.add("show");
    var toggle = document.querySelector("#selfAssessmentPipAccordion .self-assessment-pip-toggle");
    if (toggle) {
        toggle.classList.remove("collapsed");
        toggle.setAttribute("aria-expanded", "true");
    }
}

function refreshSavedSelfAssessmentPipCount() {
    var rowsBody = getSelfAssessmentPipRowsBody();
    var countField = getSavedSelfAssessmentPipCountField();
    if (!rowsBody || !countField) {
        return 0;
    }

    var savedCount = 0;
    var rows = rowsBody.querySelectorAll("tr.self-assessment-pip-row");
    for (var i = 0; i < rows.length; i++) {
        if ((rows[i].getAttribute("data-pip-id") || "").trim() !== "") {
            savedCount += 1;
        }
    }
    countField.value = savedCount;
    updateSelfAssessmentPipCountLabel(savedCount);
    return savedCount;
}

function isSelfAssessmentPipRowEditing(row) {
    return row && row.classList.contains("is-edit-mode");
}

function setSelfAssessmentPipText(row, selector, value) {
    var element = row ? row.querySelector(selector) : null;
    if (element) {
        element.textContent = value || "";
    }
}

function getSelectedSelfAssessmentTargetText(row) {
    var targetField = row ? row.querySelector(".self-assessment-target") : null;
    if (!targetField || targetField.selectedIndex < 0 || !targetField.options[targetField.selectedIndex]) {
        return "No linked target";
    }
    var selectedValue = targetField.value || "";
    return selectedValue === "" ? "No linked target" : targetField.options[targetField.selectedIndex].text;
}

function readSelfAssessmentPipData(row) {
    var notApplicableField = row ? row.querySelector(".self-assessment-not-applicable") : null;
    return {
        targetId: readSelfAssessmentPipField(row, ".self-assessment-target"),
        targetText: getSelectedSelfAssessmentTargetText(row),
        targetArea: readSelfAssessmentPipField(row, ".self-assessment-target-area"),
        concern: readSelfAssessmentPipField(row, ".self-assessment-concern"),
        expectedStandard: readSelfAssessmentPipField(row, ".self-assessment-expected-standard"),
        agreedAction: readSelfAssessmentPipField(row, ".self-assessment-agreed-action"),
        requiredSupport: readSelfAssessmentPipField(row, ".self-assessment-required-support"),
        endDate: readSelfAssessmentPipField(row, ".self-assessment-end-date"),
        notApplicable: notApplicableField ? notApplicableField.checked : false
    };
}

function applySelfAssessmentPipDataToControls(row, data) {
    if (!row || !data) {
        return;
    }
    var targetField = row.querySelector(".self-assessment-target");
    if (targetField && data.targetId !== undefined) {
        targetField.value = data.targetId || "";
    }
    var fieldMap = {
        ".self-assessment-target-area": "targetArea",
        ".self-assessment-concern": "concern",
        ".self-assessment-expected-standard": "expectedStandard",
        ".self-assessment-agreed-action": "agreedAction",
        ".self-assessment-required-support": "requiredSupport",
        ".self-assessment-end-date": "endDate"
    };
    for (var selector in fieldMap) {
        if (!Object.prototype.hasOwnProperty.call(fieldMap, selector)) {
            continue;
        }
        var field = row.querySelector(selector);
        var key = fieldMap[selector];
        if (field && data[key] !== undefined && data[key] !== null) {
            field.value = data[key];
        }
    }
    var notApplicableField = row.querySelector(".self-assessment-not-applicable");
    if (notApplicableField && data.notApplicable !== undefined) {
        notApplicableField.checked = data.notApplicable === true;
    }
}

function writeSelfAssessmentPipDisplay(row) {
    if (!row) {
        return;
    }
    var data = readSelfAssessmentPipData(row);
    setSelfAssessmentPipText(row, ".self-assessment-target-text", data.targetText);
    setSelfAssessmentPipText(row, ".self-assessment-target-area-text", data.targetArea);
    setSelfAssessmentPipText(row, ".self-assessment-concern-text", data.concern);
    setSelfAssessmentPipText(row, ".self-assessment-expected-standard-text", data.expectedStandard);
    setSelfAssessmentPipText(row, ".self-assessment-agreed-action-text", data.agreedAction);
    setSelfAssessmentPipText(row, ".self-assessment-required-support-text", data.requiredSupport);
    setSelfAssessmentPipText(row, ".self-assessment-end-date-text", data.endDate);
    setSelfAssessmentPipText(row, ".self-assessment-not-applicable-text", data.notApplicable ? "Yes" : "No");
}

function setSelfAssessmentPipRowMode(row, editMode) {
    if (!row) {
        return;
    }
    row.classList.toggle("is-edit-mode", editMode);
    row.classList.toggle("is-display-mode", !editMode);

    var editControls = row.querySelectorAll(".self-assessment-pip-edit-control");
    for (var i = 0; i < editControls.length; i++) {
        editControls[i].disabled = !editMode;
    }
    toggleSelfAssessmentNotApplicable(row.querySelector(".self-assessment-not-applicable"));
}

function removeSelfAssessmentPipEmptyRow() {
    var rowsBody = getSelfAssessmentPipRowsBody();
    var emptyRow = rowsBody ? rowsBody.querySelector(".self-assessment-pip-empty-row") : null;
    if (emptyRow && emptyRow.parentNode) {
        emptyRow.parentNode.removeChild(emptyRow);
    }
}

function ensureSelfAssessmentPipEmptyRow() {
    var rowsBody = getSelfAssessmentPipRowsBody();
    if (!rowsBody || rowsBody.querySelector("tr.self-assessment-pip-row")) {
        return;
    }
    if (rowsBody.querySelector(".self-assessment-pip-empty-row")) {
        return;
    }
    var emptyRow = document.createElement("tr");
    emptyRow.className = "self-assessment-pip-empty-row";
    var cell = document.createElement("td");
    cell.colSpan = 9;
    cell.textContent = "No training and development interventions saved.";
    emptyRow.appendChild(cell);
    rowsBody.appendChild(emptyRow);
}

function addSelfAssessmentPipRow() {
    var rowsBody = getSelfAssessmentPipRowsBody();
    var rowTemplate = document.getElementById("selfAssessmentPipRowTemplate");
    if (!rowsBody || !rowTemplate) {
        setSelfAssessmentPipStatus("Unable to add intervention row.", true);
        return;
    }

    var newRow;
    if (rowTemplate.content) {
        newRow = rowTemplate.content.firstElementChild.cloneNode(true);
    } else {
        var wrapper = document.createElement("tbody");
        wrapper.innerHTML = rowTemplate.innerHTML;
        newRow = wrapper.querySelector("tr");
    }
    removeSelfAssessmentPipEmptyRow();
    rowsBody.appendChild(newRow);
    setSelfAssessmentPipRowMode(newRow, true);
    var firstField = newRow.querySelector(".self-assessment-pip-edit-control:not([disabled])");
    if (firstField && typeof firstField.focus === "function") {
        firstField.focus();
    }
    setSelfAssessmentPipStatus("", false);
}

function ensureSelfAssessmentPipBlankRow() {
    ensureSelfAssessmentPipEmptyRow();
}

function toggleSelfAssessmentNotApplicable(checkbox) {
    if (!checkbox) {
        return;
    }
    var row = checkbox.closest("tr");
    if (!row) {
        return;
    }

    var isNotApplicable = checkbox.checked;
    var isEditing = isSelfAssessmentPipRowEditing(row);
    var targetField = row.querySelector(".self-assessment-target");
    if (targetField) {
        if (isNotApplicable && isEditing) {
            targetField.value = "";
        }
        targetField.disabled = !isEditing || isNotApplicable;
    }

    var textFields = row.querySelectorAll(".self-assessment-na-fill");
    for (var i = 0; i < textFields.length; i++) {
        if (isNotApplicable) {
            textFields[i].value = "N/A";
            textFields[i].readOnly = true;
        } else {
            if (isEditing && textFields[i].value === "N/A") {
                textFields[i].value = "";
            }
            textFields[i].readOnly = false;
        }
        textFields[i].disabled = !isEditing;
    }

    var dateField = row.querySelector(".self-assessment-end-date");
    if (dateField) {
        if (isNotApplicable && isEditing) {
            dateField.value = "";
        }
        dateField.readOnly = isNotApplicable;
        dateField.disabled = !isEditing;
    }
    checkbox.disabled = !isEditing;
}

function readSelfAssessmentPipField(row, selector) {
    var field = row ? row.querySelector(selector) : null;
    return field ? field.value : "";
}

function applySelfAssessmentPipResponse(row, response) {
    if (!row || !response) {
        return;
    }
    if (response.id !== undefined && response.id !== null) {
        row.setAttribute("data-pip-id", response.id);
    }
    applySelfAssessmentPipDataToControls(row, response);
    writeSelfAssessmentPipDisplay(row);
    setSelfAssessmentPipRowMode(row, false);
    row.selfAssessmentPipEditSnapshot = null;
    refreshSavedSelfAssessmentPipCount();
}

function editSelfAssessmentPip(button) {
    var row = button ? button.closest("tr") : null;
    if (!row) {
        return;
    }
    row.selfAssessmentPipEditSnapshot = readSelfAssessmentPipData(row);
    setSelfAssessmentPipRowMode(row, true);
    var firstField = row.querySelector(".self-assessment-pip-edit-control:not([disabled])");
    if (firstField && typeof firstField.focus === "function") {
        firstField.focus();
    }
    setSelfAssessmentPipStatus("", false);
}

function cancelSelfAssessmentPipEdit(button) {
    var row = button ? button.closest("tr") : null;
    if (!row) {
        return;
    }
    var pipId = (row.getAttribute("data-pip-id") || "").trim();
    if (pipId === "") {
        row.parentNode.removeChild(row);
        ensureSelfAssessmentPipEmptyRow();
        refreshSavedSelfAssessmentPipCount();
        setSelfAssessmentPipStatus("", false);
        return;
    }
    if (row.selfAssessmentPipEditSnapshot) {
        applySelfAssessmentPipDataToControls(row, row.selfAssessmentPipEditSnapshot);
    }
    writeSelfAssessmentPipDisplay(row);
    setSelfAssessmentPipRowMode(row, false);
    row.selfAssessmentPipEditSnapshot = null;
    setSelfAssessmentPipStatus("", false);
}

function saveSelfAssessmentPip(button) {
    var row = button ? button.closest("tr") : null;
    var scorecardIdField = document.getElementById("id");
    if (!row || !scorecardIdField) {
        setSelfAssessmentPipStatus("Unable to locate intervention fields.", true);
        return;
    }

    var notApplicableField = row.querySelector(".self-assessment-not-applicable");
    var payload = {
        id: row.getAttribute("data-pip-id") || "",
        scorecardId: scorecardIdField.value,
        targetId: readSelfAssessmentPipField(row, ".self-assessment-target"),
        targetArea: readSelfAssessmentPipField(row, ".self-assessment-target-area"),
        concern: readSelfAssessmentPipField(row, ".self-assessment-concern"),
        expectedStandard: readSelfAssessmentPipField(row, ".self-assessment-expected-standard"),
        agreedAction: readSelfAssessmentPipField(row, ".self-assessment-agreed-action"),
        requiredSupport: readSelfAssessmentPipField(row, ".self-assessment-required-support"),
        endDate: readSelfAssessmentPipField(row, ".self-assessment-end-date"),
        notApplicable: notApplicableField ? notApplicableField.checked : false
    };

    var csrfToken = getCsrfToken();
    if (csrfToken) {
        payload._csrf = csrfToken;
    }

    $.ajax({
        url: "/scorecards/save-self-assessment-pip",
        method: "POST",
        data: payload,
        beforeSend: function (xhr) {
            if (csrfToken) {
                xhr.setRequestHeader("X-XSRF-TOKEN", csrfToken);
            }
        },
        success: function (data) {
            var response = parseAjaxResponse(data);
            if (response.saved !== true) {
                setSelfAssessmentPipStatus(response.message || "Development intervention was not saved.", true);
                return;
            }
            applySelfAssessmentPipResponse(row, response);
            setSelfAssessmentPipStatus(response.message || "Development intervention saved.", false);
        },
        error: function (xhr) {
            var response = parseAjaxResponse(xhr.responseText);
            setSelfAssessmentPipStatus(response.message || "Development intervention save failed. Please retry.", true);
        }
    });
}

var pendingSelfAssessmentPipDeleteButton = null;

function confirmDeleteSelfAssessmentPip(button) {
    pendingSelfAssessmentPipDeleteButton = button;
    var modal = typeof $ === "undefined" ? null : $("#selfAssessmentPipDeleteConfirmModal");
    if (modal && modal.length && typeof modal.modal === "function") {
        modal.modal("show");
        return;
    }
    if (confirm("Delete this training and development intervention?")) {
        deleteConfirmedSelfAssessmentPip();
    }
}

function deleteConfirmedSelfAssessmentPip() {
    if (!pendingSelfAssessmentPipDeleteButton) {
        return;
    }
    var button = pendingSelfAssessmentPipDeleteButton;
    pendingSelfAssessmentPipDeleteButton = null;
    var modal = typeof $ === "undefined" ? null : $("#selfAssessmentPipDeleteConfirmModal");
    if (modal && modal.length && typeof modal.modal === "function") {
        modal.modal("hide");
    }
    deleteSelfAssessmentPip(button);
}

function deleteSelfAssessmentPip(button) {
    var row = button ? button.closest("tr") : null;
    var scorecardIdField = document.getElementById("id");
    if (!row) {
        return;
    }

    var pipId = (row.getAttribute("data-pip-id") || "").trim();
    if (pipId === "") {
        row.parentNode.removeChild(row);
        ensureSelfAssessmentPipBlankRow();
        refreshSavedSelfAssessmentPipCount();
        setSelfAssessmentPipStatus("", false);
        return;
    }
    if (!scorecardIdField) {
        setSelfAssessmentPipStatus("Unable to resolve scorecard.", true);
        return;
    }

    var payload = {
        id: pipId,
        scorecardId: scorecardIdField.value
    };
    var csrfToken = getCsrfToken();
    if (csrfToken) {
        payload._csrf = csrfToken;
    }

    $.ajax({
        url: "/scorecards/delete-self-assessment-pip",
        method: "POST",
        data: payload,
        beforeSend: function (xhr) {
            if (csrfToken) {
                xhr.setRequestHeader("X-XSRF-TOKEN", csrfToken);
            }
        },
        success: function (data) {
            var response = parseAjaxResponse(data);
            if (response.saved !== true) {
                setSelfAssessmentPipStatus(response.message || "Development intervention was not deleted.", true);
                return;
            }
            row.parentNode.removeChild(row);
            ensureSelfAssessmentPipBlankRow();
            refreshSavedSelfAssessmentPipCount();
            setSelfAssessmentPipStatus(response.message || "Development intervention deleted.", false);
        },
        error: function (xhr) {
            var response = parseAjaxResponse(xhr.responseText);
            setSelfAssessmentPipStatus(response.message || "Development intervention delete failed. Please retry.", true);
        }
    });
}

function validateSelfAssessmentPipSubmission() {
    var countField = getSavedSelfAssessmentPipCountField();
    if (!countField) {
        return true;
    }
    var savedCount = refreshSavedSelfAssessmentPipCount();
    if (savedCount < 1) {
        openSelfAssessmentPipPanel();
        setSelfAssessmentPipStatus("Save at least one training and development intervention, or save N/A if none is required.", true);
        return false;
    }
    return true;
}

function validateScoreCaptureSubmission() {
    var scoreFields = document.querySelectorAll("input[id^='employeeScore'], input[id^='managerScore'], input[id^='agreedScore']");
    for (var i = 0; i < scoreFields.length; i++) {
        if (!validateScoreCaptureInput(scoreFields[i], true)) {
            return false;
        }
    }
    return true;
}

function validateScoreCaptureFormSubmission(requireSelfAssessmentPip) {
    if (!validateScoreCaptureSubmission()) {
        return false;
    }
    if (requireSelfAssessmentPip === true && !validateSelfAssessmentPipSubmission()) {
        return false;
    }
    return true;
}

function initializeSelfAssessmentPipRows() {
    var rowsBody = getSelfAssessmentPipRowsBody();
    if (!rowsBody) {
        return;
    }
    var rows = rowsBody.querySelectorAll("tr.self-assessment-pip-row");
    for (var i = 0; i < rows.length; i++) {
        writeSelfAssessmentPipDisplay(rows[i]);
        setSelfAssessmentPipRowMode(rows[i], isSelfAssessmentPipRowEditing(rows[i]));
    }
    ensureSelfAssessmentPipEmptyRow();
    refreshSavedSelfAssessmentPipCount();
}

function handleScoreCaptureInputChange(event) {
    var scoreField = event ? event.target : null;
    if (!isScoreCaptureInput(scoreField)) {
        return;
    }
    validateScoreCaptureInput(scoreField, false);
}

function initializeScoreCaptureFieldValidation() {
    if (window.scoreCaptureFieldValidationInitialized === true) {
        return;
    }
    window.scoreCaptureFieldValidationInitialized = true;
    document.addEventListener("input", handleScoreCaptureInputChange, true);
    document.addEventListener("change", handleScoreCaptureInputChange, true);

    var existingFields = document.querySelectorAll("input[id^='employeeScore'], input[id^='managerScore'], input[id^='agreedScore'], input#overallScore");
    for (var i = 0; i < existingFields.length; i++) {
        if (String(existingFields[i].value || "").trim() !== "" && isScoreOutOfRange(existingFields[i].value)) {
            setScoreFieldStatus(existingFields[i], "Score must be between 1 and 5.", true);
        }
    }
}

function initializeScoreCapturePage() {
    initializeSelfAssessmentPipRows();
    initializeScoreCaptureFieldValidation();
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initializeScoreCapturePage);
} else {
    initializeScoreCapturePage();
}

var commentDebounceTimers = {};

function saveComment(userType) {
    var scorecardIdField = document.getElementById("id");
    if (!scorecardIdField) {
        return;
    }

    var commentFieldMap = {
        owner: "ownerComment",
        supervisor: "supervisorComment",
        moderator: "moderatorComment"
    };
    var normalizedUserType = (userType || "").toLowerCase();
    var commentFieldId = commentFieldMap[normalizedUserType];
    if (!commentFieldId) {
        return;
    }

    var commentField = document.getElementById(commentFieldId);
    if (!commentField) {
        return;
    }

    if (commentDebounceTimers[normalizedUserType]) {
        clearTimeout(commentDebounceTimers[normalizedUserType]);
    }
    commentDebounceTimers[normalizedUserType] = setTimeout(function () {
        var csrfToken = getCsrfToken();
        var payload = {
            scorecardId: scorecardIdField.value,
            userType: userType,
            comment: commentField.value
        };
        if (csrfToken) {
            payload._csrf = csrfToken;
        }

        $.ajax({
            url: "/scorecards/save-overall-comment",
            method: "POST",
            data: payload,
            beforeSend: function (xhr) {
                if (csrfToken) {
                    xhr.setRequestHeader("X-XSRF-TOKEN", csrfToken);
                }
            },
            success: function () {
                setScoreStatus("Comment saved.", false);
            },
            error: function () {
                setScoreStatus("Comment save failed. Please retry.", true);
            }
        });
    }, 600);
}
