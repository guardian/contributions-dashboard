var colours = {};               //Assigns a colour to each country with recent contributions
var contributions = {};         //Represents each country as an array of timestamps

const cutOffMillis = 60000*2;
const baseCountryColour = '#ffffff';
const hoverCountryColour = '#22874d';

const lowContributionsColour = '#ffe500';
const mediumContributionsColour = '#e05e00';
const highContributionsColour = '#c70000';

const getColour = (length) => {
    if (length < 1) return baseCountryColour;
    if (length === 1) return lowContributionsColour;
    if (length === 2) return mediumContributionsColour;
    return highContributionsColour
};

const buildLink = url => `<a href='${url}'>${url.split('/').pop()}</a>`;

const buildRow = (contribution) =>
    '<li>' +
        `<span class='time'>${new Date().toLocaleTimeString()}</span>: `+
        `<span class='amount'>${contribution.amount} ${contribution.currency}</span> `+
        `(${contribution.paymentFrequency.toLowerCase()} from ${contribution.countryCode}) `+
        (contribution.referrerUrl ? ` - ${buildLink(contribution.referrerUrl)}` : '') +
    '</li>';

const isContribution = (contribution) => contribution.product === 'CONTRIBUTION' || contribution.product === 'RECURRING_CONTRIBUTION';

const updateMap = () => $('#world-map').vectorMap('set','colors', colours);

const addToContributions = (country) => {
    if (contributions[country]) contributions[country].push(new Date());
    else contributions[country] = [new Date()];

    colours[country] = getColour(contributions[country].length);
    updateMap();
};

const expireContributions = () => {
    const cutOff = new Date() - cutOffMillis;

    Object.keys(contributions).forEach(country => {
        //Drop any contributions earlier than cutOff
        const idx = contributions[country].findIndex(c => c >= cutOff);
        if (idx > 0) contributions[country] = contributions[country].slice(idx);
        else if (idx < 0) {
            delete contributions[country];
            colours[country] = baseCountryColour;
            updateMap();
        }
    });
};

const setTopPerforming = (topPerforming) => {
    const list = $('#top-content ol');
    list.empty();

    topPerforming
        .forEach(content => list.append(`<li><span class="av">£${content.annualisedValueInGBP} AV</span> - ${content.url}</li>`));
};

window.onload = () => {

    window.setInterval(expireContributions, 10000);

    $('#world-map').vectorMap({
        map: 'world_en',
        color: baseCountryColour,
        selectedColor: hoverCountryColour,
        hoverColor: hoverCountryColour,
        showTooltip: true,
        onRegionClick: (event, code, region) => {
            console.log(`Contributions in ${code}: ${contributions[code]}`)
        },
        onLabelShow: (event, label, code) => {
            if (contributions[code]) {
                label.text(`${code.toUpperCase()}: ${contributions[code].length} contributions`);
            } else {
                label.text(`${code.toUpperCase()}: 0 contributions`);
            }
        }
    });

    const connect = () => new WebSocket('ws://localhost:9000/socket');

    var socket = connect();

    const eventsList = $('#events ul');

    socket.onopen = event => {
        socket.send('testing')
    };

    socket.onmessage = event => {
        const data = JSON.parse(event.data)
        console.log('received:', data)

        switch(data.messageType) {
            case 'acquisition':
                if (isContribution(data.payload)) {
                    if (eventsList.children().length > 15) {
                        $('li:last-child', eventsList).remove();
                    }
                    eventsList.prepend(buildRow(data.payload));

                    const country = data.payload.countryCode.toLowerCase();
                    addToContributions(country);
                }
                return;

            case 'rankIndex':
                setTopPerforming(data.payload);
                return;

            default:
                return;
        }
    };

    socket.onclose = () => {
        console.log("websocket closed - reconnecting")
        socket = connect();
    };

    socket.onerror = (err) => {
        console.log("websocket error - reconnecting")
        socket = connect();
    }
};
