Modpack info file [1]:
	<MODPACK NICE NAME> (displayed to user on technic site and launcher)
	<MODPACK LINK> (unknown purpose)
	<RECOMMENDED/LATEST VERSION> (used in the launcher) (TODO: make these separate options)

Modpack version info file [2]:
	<MINECRAFT VERSION> (tells launcher what version to use)
	<MINECRAFT MD5> (unknown purpose)
	<FORGE VERSION> (unknown purpose, you can use a 'mod' to apply forge)

Mod info file [3]:
	<SLUG> (small identifier for the mod)
	<VERSION> (used for updating a modpack to reduce downloads)
	<MD5> (used to spot download errors, you can leave this blank)
	<PRETTY NAME> (used on the technic site)
	<AUTHOR> (used on the technic site)
	<LINK> (used on the technic site)
	<DESCRIPTION> (used on the technic site, multiline)


    How to use:
1. Make a "modpacks" folder
2. For each modpack, do the following:
    a. Make a folder in "modpacks" for that modpack
    b. In this folder, make an "info.txt" file and fill it out as shown above [1]
    c. Optionally, add "background.jpg"(880x520), "icon.png"(54x54), and "logo_180.png"(180x110)
    d. For each version, do the following:
        1. Make a folder with the name of the version
        2. In this folder, make an "info.txt" file and fill it out as shown above [2]
        3. Make a "mods" folder
        4. In this folder, make a text file (any name you want) for each mod and fill it out as shown above [3]