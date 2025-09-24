drop database if exists testtemp;
create database testtemp;
use testtemp;

create table temperature (
    id int not null auto_increment primary key,
    value double not null,
    created_at datetime default current_timestamp,
    update_at datetime default current_timestamp on update current_timestamp
);