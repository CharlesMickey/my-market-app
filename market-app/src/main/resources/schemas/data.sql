DELETE FROM order_items;

DELETE FROM orders;

DELETE FROM items;

INSERT INTO
    items (title, description, img_path, price)
VALUES
    (
        'Футбольный мяч',
        'Профессиональный футбольный мяч для матчей',
        'images/q.jpg',
        2999
    ),
    (
        'Баскетбольный мяч',
        'Мяч для игры в баскетбол',
        'images/q.jpg',
        3499
    ),
    (
        'Теннисная ракетка',
        'Профессиональная ракетка для большого тенниса',
        'images/q.jpg',
        7999
    );