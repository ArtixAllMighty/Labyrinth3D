package labyrinth3D.gamestates.maze3D;


import java.util.ArrayList;

import labyrinth3D.game.entity.Entity;
import labyrinth3D.rscMngr.Texture;
import labyrinth3D.rscMngr.TextureLoader;

public class Screen {
	public int[][] map;
	public int mapWidth, mapHeight, width, height;
	public ArrayList<Texture> wallTextures = new ArrayList<Texture>();
	public ArrayList<Texture> entityTextures = new ArrayList<Texture>();

	public double[] ZBuffer;

	int texIndex = 0;

	double spriteWorldPosX;
	double spriteWorldPosY;

	double transformX;
	double transformY;

	int spriteHeight;
	int spriteWidth;

	int drawStartY;
	int drawEndY;

	int drawStartX;
	int drawEndX;

	Entity e;
	Texture texture;

	int texSize;

	public Screen(int[][] m, int mapW, int mapH, int w, int h) {
		map = m;
		mapWidth = mapW;
		mapHeight = mapH;
		width = w;
		height = h;

		ZBuffer = new double[w];

		filltextureArray();

	}

	public int[] drawScreen(Camera camera, int[] pixels, ArrayList<Entity> entity, ArrayList<Entity> enemy) {

//		//floor colors. top and bottom
//		for(int n=0; n<pixels.length/2; n++) {
//			if(pixels[n] != Color.DARK_GRAY.getRGB()) pixels[n] = Color.DARK_GRAY.getRGB();
//		}
//
//		for(int i=pixels.length/2; i<pixels.length; i++){
//			if(pixels[i] != Color.GRAY.getRGB()) pixels[i] = Color.GRAY.getRGB();
//		}

		//casting the walls
		drawWalls(camera, pixels);

		drawEntities(entity, camera, pixels);
		drawEntities(enemy, camera, pixels);

		return null;
	}

	private void drawWalls(Camera camera , int[] pixels) {
		//loop trough screen width
		for(int x=0; x < width; x++) {

			//2 stretches the game a bit. might be the 1024x576 screen setting
			double cameraX = 2 * x / (double)(width) -1;

			//raycast Point Of View
			double rayDirX = camera.xDir + camera.xPlane * cameraX;
			double rayDirY = camera.yDir + camera.yPlane * cameraX;

			//Map position, where the camera is in the map
			int mapX = (int)camera.xPos;
			int mapY = (int)camera.yPos;

			//length of ray from current position to next x or y-side
			double sideDistX;
			double sideDistY;

			//Length of ray from one side to next in map
			double deltaDistX = Math.sqrt(1 + (rayDirY*rayDirY) / (rayDirX*rayDirX));
			double deltaDistY = Math.sqrt(1 + (rayDirX*rayDirX) / (rayDirY*rayDirY));

			double perpWallDist; //perpendicular wall distance

			//Direction to go in x and y
			int stepX, stepY;

			boolean hit = false;//was a wall hit

			int side=0;//was the wall vertical or horizontal

			//Figure out the step direction and initial distance to a side
			if (rayDirX < 0)
			{
				stepX = -1;
				sideDistX = (camera.xPos - mapX) * deltaDistX;
			}
			else
			{
				stepX = 1;
				sideDistX = (mapX + 1.0 - camera.xPos) * deltaDistX;
			}
			if (rayDirY < 0)
			{
				stepY = -1;
				sideDistY = (camera.yPos - mapY) * deltaDistY;
			}
			else
			{
				stepY = 1;
				sideDistY = (mapY + 1.0 - camera.yPos) * deltaDistY;
			}

			//Loop to find where the ray hits a wall
			while(!hit) {
				//Jump to next square
				if (sideDistX < sideDistY)
				{
					sideDistX += deltaDistX;
					mapX += stepX;
					side = 0;
				}
				else
				{
					sideDistY += deltaDistY;
					mapY += stepY;
					side = 1;
				}
				//Check if ray has hit a wall

				if(map[mapX][mapY] > 0) 
					hit = true;

			}

			//Calculate distance to the point of impact
			//math abs(olute) turns negative nbrs into positive numbers
			if(side==0)
				perpWallDist = Math.abs((mapX - camera.xPos + (1 - stepX) / 2) / rayDirX);
			else
				perpWallDist = Math.abs((mapY - camera.yPos + (1 - stepY) / 2) / rayDirY);	

			//Now calculate the height of the wall based on the distance from the camera
			int lineHeight;

			if(perpWallDist > 0) 
				lineHeight = Math.abs((int)(height / perpWallDist));
			else 
				lineHeight = height;

			//calculate lowest and highest pixel to fill in current stripe
			int drawStart = -lineHeight/2+ height/2;

			if(drawStart < 0)
				drawStart = 0;

			int drawEnd = lineHeight/2 + height/2;

			if(drawEnd > height) 
				drawEnd = height;

			//add a texture
			int texNum = map[mapX][mapY] - 1;

			//texture
			Texture texture = wallTextures.get(texNum);

			double wallX;//Exact position of where wall was hit

			if(side==1) {//If its a y-axis wall
				wallX = (camera.xPos + ((mapY - camera.yPos + (1 - stepY) / 2) / rayDirY) * rayDirX);
			} else {//X-axis wall
				wallX = (camera.yPos + ((mapX - camera.xPos + (1 - stepX) / 2) / rayDirX) * rayDirY);
			}

			wallX-=Math.floor(wallX);

			//x coordinate on the texture
			int texX = (int)(wallX * (wallTextures.get(texNum).SIZE));

			if(side == 0 && rayDirX > 0)
				texX = wallTextures.get(texNum).SIZE - texX - 1;

			if(side == 1 && rayDirY < 0) 
				texX = wallTextures.get(texNum).SIZE - texX - 1;

			//calculate y coordinate on texture
			for(int y=drawStart; y<drawEnd; y++) {
				int texY = (((y*2 - height + lineHeight) << 6) / lineHeight) / 2;
				int color;
				int sum = texX + (texY * texture.SIZE);

				if(sum > texture.SIZE*texture.SIZE )
					sum = (texture.SIZE*texture.SIZE) -1;

				if(sum < 0)
					sum = 0;

				if(side==0)
					color = texture.pixels[sum];
				else
					color = (texture.pixels[sum]>>1) & 8355711;//Make y sides darker

				pixels[x + y*(width)] = color;
			}

			ZBuffer[x] = perpWallDist;

			//FLOOR CASTING
			double floorXWall, floorYWall; //x, y position of the floor texel at the bottom of the wall

			//4 different wall directions possible
			if(side == 0 && rayDirX > 0)
			{
				floorXWall = mapX;
				floorYWall = mapY + wallX;
			}
			else if(side == 0 && rayDirX < 0)
			{
				floorXWall = mapX + 1.0;
				floorYWall = mapY + wallX;
			}
			else if(side == 1 && rayDirY > 0)
			{
				floorXWall = mapX + wallX;
				floorYWall = mapY;
			}
			else
			{
				floorXWall = mapX + wallX;
				floorYWall = mapY + 1.0;
			}

			double distWall, distPlayer, currentDist;

			distWall = perpWallDist;
			distPlayer = 0.0;
			if (drawEnd < 0) drawEnd = height; //becomes < 0 when the integer overflows
			
			Texture ceilTex = TextureLoader.ceiling;
			Texture floorTex = TextureLoader.floor;

			//draw the floor from drawEnd to the bottom of the screen
			for(int y = drawEnd + 1; y < height; y++)
			{
				currentDist = height / (2.0 * y - height); //you could make a small lookup table for this instead

				double weight = (currentDist - distPlayer) / (distWall - distPlayer);

				double currentFloorX = weight * floorXWall + (1.0 - weight) * camera.xPos;
				double currentFloorY = weight * floorYWall + (1.0 - weight) * camera.yPos;

				int floorTexX, floorTexY;
				floorTexX = (int)(currentFloorX * 64) % 64;
				floorTexY = (int)(currentFloorY * 64) % 64;

				//floor
				pixels[x+y*width] = floorTex.pixels[64 * floorTexY + floorTexX] >> 1 & 8355711;
				//ceiling (symmetrical!)
				pixels[(x+((height-y)*width))] = ceilTex.pixels[64 * floorTexY + floorTexX];
			}
		}
	}

	private void drawEntities(ArrayList<Entity> entity, Camera camera, int [] pixels){

		//parameters for scaling and moving the sprites
		double uDiv = 1;
		double vDiv = 1;
		double vMove = 0.0;
		int vMoveScreen;

		//required for correct matrix multiplication
		double invDet = 1.0 / (camera.xPlane * camera.yDir - camera.xDir * camera.yPlane); 


		for(int entities = 0; entities < entity.size(); entities++){

			e = entity.get(entities);

			uDiv = e.getScaleX();
			vDiv = e.getScaleY();
			vMove = e.getMoveOffset();

			texture = e.getTexture();

			texSize = texture.SIZE;

			//translate sprite position relative to camera
			spriteWorldPosX = e.worldPositionX - camera.xPos;
			spriteWorldPosY = e.worldPositionY - camera.yPos;

			//transform sprite with the inverse camera matrix
			// [ planeX   dirX ] -1                                       [ dirY      -dirX ]
			// [               ]       =  1/(planeX*dirY-dirX*planeY) *   [                 ]
			// [ planeY   dirY ]                                          [ -planeY  planeX ]

			transformX = invDet * (camera.yDir * spriteWorldPosX - camera.xDir * spriteWorldPosY);
			//this is actually the depth inside the screen, that what Z is in 3D
			transformY = invDet * (-camera.yPlane * spriteWorldPosX + camera.xPlane * spriteWorldPosY); 

			vMoveScreen = (int) (vMove / transformY);

			int spriteScreenX = (int) ((width / 2) * (1 + transformX / transformY));

			//calculate height of the sprite on screen

			//using "transformY" instead of the real distance prevents fisheye
			spriteHeight = (int) (Math.abs((height / (transformY))) / vDiv); 
			spriteWidth  = (int) (Math.abs((height / (transformY))) / uDiv);

			//calculate lowest and highest pixel to fill in current stripe
			drawStartY = (-spriteHeight / 2 + height / 2) + vMoveScreen;
			//calculate width of the sprite
			drawEndY = (spriteHeight / 2 + height / 2) + vMoveScreen;

			drawStartX = -spriteWidth / 2 + spriteScreenX;
			drawEndX = spriteWidth / 2 + spriteScreenX;

			if(drawStartY < 0) 
				drawStartY = 0;
			if(drawEndY >= height) 
				drawEndY = height - 1;

			if(drawStartX < 0) 
				drawStartX = 0;
			if(drawEndX >= width)
				drawEndX = width - 1;

			//loop through every vertical stripe of the sprite on screen
			for(int stripe = drawStartX; stripe < drawEndX; stripe++){
				//the conditions in the if are:
				//1) it's in front of camera plane so you don't see things behind you, 2) it's on the screen (left)
				//3) it's on the screen (right), 4) ZBuffer, with perpendicular distance

				if(transformY > 0 && stripe > 0 && stripe < width && transformY < ZBuffer[stripe])
				{
					int textureX = (int)(256 * (stripe - (-spriteWidth / 2 + spriteScreenX)) * texSize / spriteWidth) / 256;

					loopEntityTextureHeight(stripe, textureX, vMoveScreen, texture, pixels);
				}
			}
		}
	}

	private void loopEntityTextureHeight(int stripe, int textureX, int vMoveScreen, Texture texture, int[] pixels) {

		for(int y = drawStartY; y < drawEndY; y++){

			int d = (y - vMoveScreen) * 256 - height * 128 + spriteHeight * 128;
			int textureY = ((d * texSize) / spriteHeight) / 256;

			int sum = (int)textureY*texSize + (int)textureX;

			if(sum < 0)
				sum = 0;
			if(sum > texSize*texSize)
				sum = (texSize*texSize)-1;

			//get current color from the texture
			int color = texture.pixels[sum]; 

			//paint pixel if it isn't transparent, black is the invisible color
			if((color >>24 & 0xff) != 0) 
				pixels[stripe + (y*width)] = color; 
		}
	}

	private void filltextureArray() {
		/////////////////////////////////////
		wallTextures.add(TextureLoader.brickStone);
		wallTextures.add(TextureLoader.brickStone1);
		wallTextures.add(TextureLoader.brickStone2);
		wallTextures.add(TextureLoader.brickStone3);
		wallTextures.add(TextureLoader.brickStone4);
		wallTextures.add(TextureLoader.brickStone5);

		/////////////////////////////////////

		entityTextures.add(TextureLoader.tex_portal);
		entityTextures.add(TextureLoader.tex_portal_1);
		entityTextures.add(TextureLoader.tex_portal_2);
		entityTextures.add(TextureLoader.tex_portal_3);

		entityTextures.add(TextureLoader.tex_statue);
		entityTextures.add(TextureLoader.tex_unicorn_blood);
		entityTextures.add(TextureLoader.tex_head);

		entityTextures.add(TextureLoader.tex_warfsword);

	}
}
