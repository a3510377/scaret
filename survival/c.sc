// 原出處: https://github.com/gnembon/scarpet/blob/master/programs/survival/cam.sc
// 修改: 猴子 (https://github.com/a3510377)

__config() -> {
    'stay_loaded' -> 'true',
    'commands' -> {
        '' -> _() -> __check_type(player()),
        '<tp_player>' -> _(tp_player) -> (
            p = player(tp_player);
            if (player() == p, return(run('tellraw @s "§4您不能旁觀自己"')));
            if (p, (
                if (player()~'gamemode' != 'spectator', __to_spectator(player()));
                run('tp ' + p~'command_name')
            ), run('tellraw @s "§4找不到玩家"'))
        )
    },
    'arguments' -> {'tp_player' -> {'type' -> 'players', 'single' -> true}},
};

__restore_player_params(player) -> (
    config = __get_store_player_data(player);

    run('execute in ' + config:'dimension' + ' run tp @s ~ ~ ~');

    modify(player, 'location', [
        ...config:'pos',
        config:'yaw',
        config:'pitch'
    ]);
    modify(player, 'gamemode', 'survival');
    modify(player, 'motion', config:'motion');

    for (config:'effects', modify(player, 'effect', _:'name', _:'duration', _:'amplifier'));

    display_title(player, 'actionbar', format('y 退出相機模式'));
    __remove_player_config(player);
);

__to_spectator(player) -> (
    __store_player_data(player);
    modify(player, 'effect');
    modify(player, 'gamemode', 'spectator');

    display_title(player, 'actionbar', format('y 進入相機模式'));
);

__check_type(player) -> (
    if (
        player~'gamemode' == 'spectator', __restore_player_params(player),
        __to_spectator(player)
    );
);

__remove_player_config(player) -> (
   tag = load_app_data();
   delete(tag:(player~'name'));
   store_app_data(tag);
);

__store_player_data(player) -> (
    tag = nbt('{}');

    for(pos(player), put(tag:'Position', str('%.6fd', _), _i)); 
    for(player~'motion', put(tag:'Motion', str('%.6fd', _), _i)); 

    tag:'Yaw' = str('%.6f', player~'yaw');
    tag:'Pitch' = str('%.6f', player~'pitch');
    tag:'Dimension' = player~'dimension';

    for (player~'effect',
        l(name, amplifier, duration) = _;
        etag = nbt('{}');
        etag:'Name' = name;
        etag:'Amplifier' = amplifier;
        etag:'Duration' = duration;
        put(tag:'Effects', etag, _i);
    );

    apptag = load_app_data();
    if (!apptag, apptag = nbt('{}'));
    apptag:(player~'name') = tag;

    store_app_data(apptag);
);

__get_store_player_data(player) -> (
    tag = load_app_data();
    if(!tag, return (null));

    player_tag = tag:(player~'name');
    if (!player_tag, return(null));

    config = m();
    config:'pos' = player_tag:'Position.[]';
    config:'motion' = player_tag:'Motion.[]';
    config:'yaw' = player_tag:'Yaw';
    config:'pitch' = player_tag:'Pitch';
    config:'dimension' = player_tag:'Dimension';
    config:'effects' = l();
    effects_tags = player_tag:'Effects.[]';

    if (effects_tags, for(effects_tags, etag = _;
        effect = m();
        effect:'name' = etag:'Name';
        effect:'amplifier' = etag:'Amplifier';
        effect:'duration' = etag:'Duration';
        config:'effects' += effect;
    ));

    config
);

__on_player_connects(player) -> if(
    __get_store_player_data(player),
    modify(player, 'gamemode' , 'spectator'
));