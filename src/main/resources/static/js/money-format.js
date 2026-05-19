(function () {
    'use strict';

    function formatCOP(value) {
        var digits = value.replace(/\D/g, '');
        if (!digits) return '';
        return parseInt(digits, 10).toLocaleString('es-CO');
    }

    function cleanCOP(value) {
        return value.replace(/[^\d]/g, '');
    }

    function handleInput(e) {
        var input = e.target;
        var cursorPos = input.selectionStart;
        var oldValue = input.value;

        var digitsBeforeCursor = oldValue.slice(0, cursorPos).replace(/\D/g, '').length;

        var newValue = formatCOP(oldValue);
        input.value = newValue;

        var newCursor = newValue.length;
        var digitsSeen = 0;
        for (var i = 0; i < newValue.length; i++) {
            if (/\d/.test(newValue[i])) {
                digitsSeen++;
                if (digitsSeen === digitsBeforeCursor) { newCursor = i + 1; break; }
            }
        }
        if (digitsBeforeCursor === 0) newCursor = 0;

        input.setSelectionRange(newCursor, newCursor);
    }

    function attachTo(input) {
        input.setAttribute('inputmode', 'numeric');
        input.setAttribute('autocomplete', 'off');

        var ph = input.getAttribute('placeholder') || '';
        if (!ph || ph === '0.00' || ph === 'Valor ($)' || ph === '0') {
            input.setAttribute('placeholder', 'Ej: 550.000');
        }

        if (input.value) {
            input.value = formatCOP(input.value);
        }

        input.addEventListener('input', handleInput);

        var form = input.closest('form');
        if (form && !form._moneyFormatBound) {
            form._moneyFormatBound = true;
            form.addEventListener('submit', function () {
                form.querySelectorAll('input.money-input').forEach(function (inp) {
                    inp.value = cleanCOP(inp.value);
                });
            });
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('input.money-input').forEach(attachTo);
    });
})();
