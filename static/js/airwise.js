document.querySelector('.nav-toggle')?.addEventListener('click', (event) => {
    const button = event.currentTarget;
    const navigation = document.querySelector('#primary-nav');
    const open = button.getAttribute('aria-expanded') === 'true';
    button.setAttribute('aria-expanded', String(!open));
    navigation.classList.toggle('is-open', !open);
});

document.querySelectorAll('.flash-dismiss').forEach((button) => {
    button.addEventListener('click', () => button.closest('.flash').remove());
});

const consoleClock = document.querySelector('#console-clock');
if (consoleClock) {
    const updateClock = () => {
        consoleClock.textContent = new Intl.DateTimeFormat(undefined, {
            weekday: 'short', day: '2-digit', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
        }).format(new Date());
    };
    updateClock();
    window.setInterval(updateClock, 1000);
}

document.querySelector('#module-filter')?.addEventListener('input', (event) => {
    const query = event.currentTarget.value.trim().toLowerCase();
    document.querySelectorAll('.console-side nav a').forEach((link) => {
        link.hidden = !link.textContent.toLowerCase().includes(query);
    });
});

document.querySelectorAll('canvas[data-chart]').forEach((canvas) => {
    const labels = JSON.parse(canvas.dataset.labels || '[]');
    const values = JSON.parse(canvas.dataset.values || '[]');
    const series = JSON.parse(canvas.dataset.series || 'null');
    const chartType = canvas.dataset.chart || 'bar';
    const colors = ['#28a9c3', '#22856e', '#d28b35', '#bb4b4b', '#667f91', '#84c8b2'];
    const datasets = series ? series.map((item, index) => ({
        label: item.label,
        data: item.data,
        backgroundColor: chartType === 'line' ? `${colors[index % colors.length]}24` : colors[index % colors.length],
        borderColor: colors[index % colors.length],
        borderWidth: 1,
        fill: false,
        tension: .25,
    })) : [{
        label: canvas.dataset.label || 'Records',
        data: values,
        backgroundColor: chartType === 'line' ? 'rgba(40, 169, 195, .14)' : colors,
        borderColor: chartType === 'line' ? '#28a9c3' : colors,
        borderWidth: 1,
        fill: chartType === 'line',
        tension: .25,
    }];
    new Chart(canvas, {
        type: chartType,
        data: { labels, datasets },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: chartType === 'pie' || chartType === 'doughnut' || datasets.length > 1, position: 'bottom' } },
            scales: chartType === 'pie' || chartType === 'doughnut' ? {} : { y: { beginAtZero: true } },
        },
    });
});