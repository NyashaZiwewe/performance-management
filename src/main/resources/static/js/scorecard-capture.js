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

function notifyScoreRangeError() {
    alert("Score must be between 1 and 5 inclusive, please update otherwise it wont be saved");
}

function isScoreOutOfRange(value) {
    if (value === "" || Number(value) === 0.0) {
        return false;
    }
    return Number(value) < 1 || Number(value) > 5;
}

function sendScoreRequest(url, payload) {
    $.ajax({
        url: url,
        method: "POST",
        data: payload,
        success: function (data) {
            let response = parseAjaxResponse(data);
            if (response.alreadyExists === true) {
                alert("Score already exists for that target/ measure and reporting date");
                return;
            }
            if (response.captureBlocked === true && response.message) {
                alert(response.message);
            }
        },
        error: function (xhr) {
            let response = parseAjaxResponse(xhr.responseText);
            if (response.message) {
                alert(response.message);
            }
        }
    });
}

function saveStandardScore(targetId) {
    let actual = document.getElementById("actual" + targetId).value;
    let evidence = document.getElementById("evidence" + targetId).value;
    let justification = document.getElementById("justification" + targetId).value;

    sendScoreRequest("/scorecards/save-standard-score", {
        targetId: targetId,
        actual: actual,
        evidence: evidence,
        justification: justification
    });
}

function saveValueBasedScore(targetId, scoreFieldIdPrefix, scoreFieldName, endpoint, justificationFieldIdPrefix) {
    let scoreValue = document.getElementById(scoreFieldIdPrefix + targetId).value;
    if (isScoreOutOfRange(scoreValue)) {
        notifyScoreRangeError();
        return false;
    }

    let payload = { targetId: targetId };
    payload[scoreFieldName] = scoreValue;

    if (justificationFieldIdPrefix) {
        payload.justification = document.getElementById(justificationFieldIdPrefix + targetId).value;
    }

    sendScoreRequest(endpoint, payload);
    return true;
}

function saveComment(userType) {
    let scorecardIdField = document.getElementById("id");
    if (!scorecardIdField) {
        return;
    }

    let commentFieldMap = {
        owner: "ownerComment",
        supervisor: "supervisorComment",
        moderator: "moderatorComment"
    };
    let commentFieldId = commentFieldMap[(userType || "").toLowerCase()];
    if (!commentFieldId) {
        return;
    }

    let commentField = document.getElementById(commentFieldId);
    if (!commentField) {
        return;
    }

    $.ajax({
        url: "/scorecards/save-overall-comment",
        method: "POST",
        data: {
            scorecardId: scorecardIdField.value,
            userType: userType,
            comment: commentField.value
        },
        success: function () {
        },
        error: function () {
        }
    });
}
