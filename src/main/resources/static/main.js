function SubForm(s) {
    $.ajax({
        url: '/qa-simple',
        type: 'post',
        data: {
            'query': s,
            'lang': 'en'
        },
        success: function (msg) {
            alert(msg);
        }
    });
}
