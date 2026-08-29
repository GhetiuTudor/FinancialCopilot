from datetime import date, timedelta


FIXED_HOLIDAYS = {
    (1, 1): "Anul Nou",
    (1, 2): "Anul Nou (zi 2)",
    (1, 24): "Ziua Unirii Principatelor Române",
    (5, 1): "Ziua Muncii",
    (6, 1): "Ziua Copilului",
    (8, 15): "Adormirea Maicii Domnului",
    (11, 30): "Sfântul Andrei",
    (12, 1): "Ziua Națională a României",
    (12, 25): "Crăciunul (zi 1)",
    (12, 26): "Crăciunul (zi 2)",
}


def orthodox_easter(year: int) -> date:
    a = year % 4
    b = year % 7
    c = year % 19
    d = (19 * c + 15) % 30
    e = (2 * a + 4 * b - d + 34) % 7
    month = (d + e + 114) // 31
    day = ((d + e + 114) % 31) + 1

    julian_date = date(year, month, day)
    gregorian_date = julian_date + timedelta(days=13)
    return gregorian_date


def get_holidays(year: int) -> set[date]:
    holidays = set()

    for (m, d) in FIXED_HOLIDAYS:
        holidays.add(date(year, m, d))

    easter = orthodox_easter(year)
    holidays.add(easter - timedelta(days=2))
    holidays.add(easter)
    holidays.add(easter + timedelta(days=1))
    holidays.add(easter + timedelta(days=49))
    holidays.add(easter + timedelta(days=50))

    return holidays


def is_romanian_holiday(d: date) -> bool:
    return d in get_holidays(d.year)
