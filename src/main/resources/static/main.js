function showSpinner() {
    $('#loaderDiv').show();
    $('#overlay').show();
}

function hideSpinner() {
    $('#loaderDiv').hide();
    $('#overlay').hide();
}

function emptyAnswerList() {
    $("#infoboxes").empty();
}

function getInfoboxValues(resource) {
    return $.ajax({
        url: 'infobox',
        timeout: 60000,
        type: 'get',
        data: {
            'resource': resource
        },
        success(msg) {
            const messageData = JSON.parse(msg)['messageData'];
            $("#infoboxes").append($(createInfobox(messageData)).hide());
        }
    });
}

function createEmptyInfobox(title) {
    $("#infoboxes").append($(createInfobox({
        "title": title
    })).hide());
}

function createInfobox(msg) {
    let divTemplate = '<div class="card"><div class="summary">';
    if (msg.hasOwnProperty('image')) {
        divTemplate += '<div class="img-wrapper"><img src="?image"></div><div class="content">'.replace('?image', msg.image);
    } else {
        divTemplate += '<div class="content-no-image">'
    }
    divTemplate += '<div class="title wrap-word">?title</div>' +
        '<div class="text wrap-word"><div>?text</div><div></div></div>' +
        '</div>';

    if (msg.hasOwnProperty('title')) {
        divTemplate = divTemplate.replace("?title", msg.title);
    } else {
        divTemplate = divTemplate.replace('?title', 'N/A');
    }
    let text = '';
    if (msg.hasOwnProperty('description')) {
        text = msg.description;
    } else if (msg.hasOwnProperty('abstract')) {
        text = msg.abstract;
    } else {
        text = '';
    }
    if (text.length > 200) {
        text = text.substring(0, 200) + '...'
    }
    divTemplate = divTemplate.replace('?text', text);
    divTemplate += '</div>';
    let buttons = msg.buttons;
    if (buttons && buttons.length > 0) {
        let buttonsDiv = '<div class="button-group">';
        for (let i in buttons) {
            if (buttons.hasOwnProperty(i)) {
                buttonsDiv += '<a href="' + buttons[i].uri + '" target="_blank" class="btn btn-block btn-primary">' + buttons[i].title + '<i class="material-icons">launch</i></a>';
            }
        }
        buttonsDiv += '</div>';
        divTemplate += buttonsDiv;
    }
    divTemplate += '</div>';
    return divTemplate;
}
function submitForm(s) {
    $.ajax({
        beforeSend() {
            showSpinner();
        },
        url: 'qa-simple',
        timeout: 60000,
        type: 'post',
        data: {
            'query': s,
            'lang': 'en'
        },
        success(msg) {
            let ajaxRequests = [];
            emptyAnswerList();
            const answer = JSON.parse(msg)['answers'];
            if (!$.isArray(answer) || !answer.length) {
                createEmptyInfobox('No answers were found.');
            } else {
                for (let i in answer) {
                    if (answer.hasOwnProperty(i)) {
                        if (answer[i].startsWith('http://dbpedia.org/resource')) {
                            ajaxRequests.push(getInfoboxValues(answer[i]));
                        } else {
                            createEmptyInfobox(answer[i]);
                        }
                    }
                }
            }

            $.when.apply(undefined, ajaxRequests).then(function () {
                $('.card').each(function (index) {
                    $(this).delay(400 * index).fadeIn(300);
                })
            });
            hideSpinner();

        },
        error(jqXHR, textStatus) {
            emptyAnswerList();
            hideSpinner();
            alert('Error while sending request or request took too long. Please try again later!');
        }
    }).fail(function (jqXHR, textStatus) {
        emptyAnswerList();
        hideSpinner();
    });
}

function init() {
    hideSpinner();
    $('#search-form-id').submit(function () {
        submitForm($('#search-bar').val());
        return false;
    });
}
