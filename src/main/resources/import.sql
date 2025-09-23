-- This file allow to write SQL commands that will be emitted in test and dev.
-- The commands are commented as their support depends of the database
-- insert into myentity (id, field) values(1, 'field-1');
-- insert into myentity (id, field) values(2, 'field-2');
-- insert into myentity (id, field) values(3, 'field-3');
-- alter sequence myentity_seq restart with 4;

INSERT INTO segment (name, description) VALUES
('Tecnologia', 'Segmento voltado a empresas de tecnologia e inovação'),
('Finanças', 'Empresas do setor financeiro, bancos e serviços correlatos'),
('Saúde', 'Empresas de produtos e serviços da área da saúde'),
('Entretenimento', 'Empresas de mídia, streaming e entretenimento digital');

INSERT INTO image (url, description) VALUES
('https://example.com/logos/google.png', 'Logo da Google'),
('https://example.com/logos/apple.png', 'Logo da Apple'),
('https://example.com/logos/banco_do_brasil.png', 'Logo do Banco do Brasil'),
('https://example.com/logos/pfizer.png', 'Logo da Pfizer'),
('https://example.com/logos/netflix.png', 'Logo da Netflix');



INSERT INTO brand (name, description, logo_id, website_url, release, segment_id) VALUES
('Google', 'Empresa líder em tecnologia e serviços on-line', 1, 'https://www.google.com', 1998, 1),
('Apple', 'Fabricante de eletrônicos, software e serviços digitais', 2, 'https://www.apple.com', 1976, 1),
('Banco do Brasil', 'Banco público brasileiro de grande porte', 3, 'https://www.bb.com.br', 1808, 2),
('Pfizer', 'Gigante da indústria farmacêutica e de saúde', 4, 'https://www.pfizer.com', 1849, 3),
('Netflix', 'Empresa líder mundial em streaming de vídeos', 5, 'https://www.netflix.com', 1997, 4);


-- alter sequence book_seq restart with 5;
