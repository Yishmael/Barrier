from PIL import Image, ImageDraw, ImageFont


names = ['healing hand', 'block', 'earthquake', 'protector', 'antlion',
         'aggression', 'healing forest', 'inferno', 'power totem',
         'entangling vines', 'executor axe']

title_font_filename = 'title.ttf'
title_font_filename = 'title2.otf'
for idx, name in enumerate(names):
    filename = name.replace(' ', '_') + '.png'
    portrait = Image.open(f'portraits/{filename}')
    if name in ['protector', 'antlion']:
        frame = Image.open(open('frame_unit.png', 'rb'))
    else:
        frame = Image.open(open('frame_util.png', 'rb'))
    image = Image.new('RGBA', frame.size)
    portrait_offset = (9, 34)
    image.paste(portrait, box=portrait_offset)
    image.paste(frame, mask=frame)
    if 'protector' in name:
        tags = ['ranged', 'timed']
        for tag_idx, tag in enumerate(tags):
            tag = Image.open(open(f'tags/{tag}.png', 'rb'))
            tag_offset = (140 - 24*tag_idx, 155)
            image.paste(tag, tag_offset, tag)
    if 'earthquake' in name:
        manacost = 2
        for i in range(manacost):
            cost = Image.open(open(f'mana.png', 'rb')).resize((24, 24))
            cost_offset = (135 - 12*i, 40)
            image.paste(cost, cost_offset, cost)
        
    draw = ImageDraw.Draw(image)
    title_font = ImageFont.truetype('fonts/' + title_font_filename, 25)
    x, y = 10, 5
    if len(name) > 11:
        title_font = ImageFont.truetype('fonts/' + title_font_filename, 20)
        y += 5
    #bbox = title_font.getmask('example string').getbbox()
    color = (20*idx, 0*idx, 255 - 20*idx)
    draw.text((x, y), name.title(), font=title_font, fill=(255, 255, 255),
              stroke_width=2, stroke_fill=(0, 0, 0))
    image.save(filename)
    
count = 0
rarities = ['common', 'uncommon', 'rare', 'epic', 'legendary']
base = 1.66
for exp, rarity in enumerate(rarities):
    c = round(base**exp)
    count += c
    #print(c, rarity)
#print('Total unique cards:', count)

