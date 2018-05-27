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

function createInfobox(msg) {
    let divTemplate = '<div class="card"><div class="summary">';
    if (msg.hasOwnProperty('image')) {
        divTemplate += '<div class="img-wrapper"><img src="?image" class="wiki-image"></div><div class="content">'.replace('?image', msg.image);
    } else {
        divTemplate += '<div class="content-no-image">';
    }
    divTemplate += '<div class="title wrap-word">?title</div>' +
        '<div class="text wrap-word font-weight-bold"><div>?description</div><div></div></div>' +
        '<div class="text wrap-word font-weight-normal"><div>?abstract</div><div></div></div>' +
        '</div>';

    if (msg.hasOwnProperty('title')) {
        divTemplate = divTemplate.replace("?title", msg.title);
    } else {
        divTemplate = divTemplate.replace('?title', 'N/A');
    }
    let text = '';
    if (msg.hasOwnProperty('description')) {
        divTemplate = divTemplate.replace('?description', msg.description);
    } else {
        divTemplate = divTemplate.replace('?description', '');
    }
    if (msg.hasOwnProperty('abstract')) {
        let abstractText = '';
        if (msg.abstract.length > 200) {
            abstractText = msg.abstract.substring(0, 200) + '...';
        }
        divTemplate = divTemplate.replace('?abstract', abstractText);
    } else {
        divTemplate = divTemplate.replace('?abstract', '');
    }
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
            let response = JSON.parse(msg);
            const answer = response['answers'];
            const sparql = response['sparql'];
            if (!$.isArray(answer) || !answer.length) {
                $('#sparql-query-content').text("");
                $('#show-sparql-button').prop('disabled', true);
                createEmptyInfobox('No answers were found.');
            } else {
                $('#sparql-query-content').text(sparql);
                $('#show-sparql-button').prop('disabled', false);
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

            $.when.apply(undefined, ajaxRequests).then(
                function () {
                    $('.card').each(function (index) {
                        $(this).delay(400 * index).fadeIn(300);
                    });
                }, function (data, textStatus, jqXHR) {
                    toastr.error('Error while fetching data from one or more answers. Please try again later or contact the admin.');
                    $('.card').each(function (index) {
                        $(this).delay(400 * index).fadeIn(300);
                    });
                });
            hideSpinner();

        },
        error(jqXHR, textStatus) {
            emptyAnswerList();
            hideSpinner();
            toastr.error('Error while sending request or request took too long. Please try again later!');
        }
    }).fail(function (jqXHR, textStatus) {
        emptyAnswerList();
        hideSpinner();
    });
}

function initExamples() {
    $('#example-1').click(function (e) {
        e.preventDefault();
        $('#search-bar').val('Where was Angela Merkel born?');
        return false;
    });
    $('#example-2').click(function (e) {
        e.preventDefault();
        $('#search-bar').val('What is the wavelength of indigo?');
        return false;
    });
    $('#example-3').click(function (e) {
        e.preventDefault();
        $('#search-bar').val('How many awards has Bertrand Russell?');
        return false;
    });
    $('#example-4').click(function (e) {
        e.preventDefault();
        $('#search-bar').val('Where is the birthplace of Goethe?');
        return false;
    });
    $('#example-5').click(function (e) {
        e.preventDefault();
        $('#search-bar').val('Where is the origin of Carolina reaper?');
        return false;
    });
    $('#example-6').click(function (e) {
        e.preventDefault();
        $('#search-bar').val('Who is the author of the interpretation of dreams?');
        return false;
    });
    $('#example-7').click(function (e) {
        e.preventDefault();
        $('#search-bar').val('From who was Adorno influenced by?');
        return false;
    });
    $('#example-8').click(function (e) {
        e.preventDefault();
        $('#search-bar').val('How much is the population of Mexico City?');
        return false;
    });
}

function init() {
    initExamples();
    hideSpinner();
    $('#search-form-id').submit(function () {
        submitForm($('#search-bar').val());
        return false;
    });

}
