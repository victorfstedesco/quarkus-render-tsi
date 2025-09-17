-- This file allow to write SQL commands that will be emitted in test and dev.
-- The commands are commented as their support depends of the database
-- insert into myentity (id, field) values(1, 'field-1');
-- insert into myentity (id, field) values(2, 'field-2');
-- insert into myentity (id, field) values(3, 'field-3');
-- alter sequence myentity_seq restart with 4;

INSERT INTO brand (id, name, description, logoUrl, websiteUrl, release) VALUES
(1, 'Nike', 'Leading global supplier of athletic shoes and apparel.', 'https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg', 'https://www.nike.com', 1),
(2, 'Adidas', 'Global company specializing in sports footwear and apparel.', 'https://upload.wikimedia.org/wikipedia/commons/2/20/Adidas_Logo.svg', 'https://www.adidas.com', 1),
(3, 'Apple', 'Technology company known for consumer electronics and software.', 'https://upload.wikimedia.org/wikipedia/commons/f/fa/Apple_logo_black.svg', 'https://www.apple.com', 1),
(4, 'Samsung', 'Multinational conglomerate specializing in electronics and appliances.', 'https://upload.wikimedia.org/wikipedia/commons/2/24/Samsung_Logo.svg', 'https://www.samsung.com', 1),
(5, 'Google', 'Global leader in internet-related products and services.', 'https://upload.wikimedia.org/wikipedia/commons/2/2f/Google_2015_logo.svg', 'https://www.google.com', 1);

-- alter sequence book_seq restart with 5;
