package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.exception.ApiError;
import com.example.demo.exception.CategoryNotFoundException;
import com.example.demo.exception.IngredientLineExistException;
import com.example.demo.exception.IngredientLineNotFoundException;
import com.example.demo.exception.RecipeExistException;
import com.example.demo.exception.RecipeNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.model.Category;
import com.example.demo.model.IngredientLine;
import com.example.demo.model.Recipe;
import com.example.demo.model.RecipeDates;
import com.example.demo.model.User;
import com.example.demo.service.CategoryService;
import com.example.demo.service.IngredientLineService;
import com.example.demo.service.RecipeService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.JsonMappingException;

@RestController
public class RecipeController {
	
	/**
	 * PARA TODAS LAS PETICIONES A /RECIPES NECESITO TENER EL TOKEN
	 */
	
	@Autowired private RecipeService recipeService;
	@Autowired private CategoryService categoryService;
	@Autowired private UserService userService;
	@Autowired private IngredientLineService ingredientLineService;
	
	//ACCESO A RECURSOS DE PRIMER NIVEL
	
	/**
	 * M??TODO que gestiona las peticiones GET a /recetas y que devuelven una lista.
	 * Busca todas las recetas del repositorio, todas las recetas por una categor??a o todas las recetas de un usuario.
	 * RequestParam --> http://localhost:9000/recipes?userID=1   --- http://localhost:9000/recipes?categoryID=1
	 * 
	 * @return 	si no tiene @requestParam, la lista de recetas del repo
	 * 			si tiene @requestParam:
			 * 		- usuario no existe --> exception UserNotFoundException
			 * 		- usuario existe y tiene recetas --> devuelve las recetas del usuario sin mostrar su info 200 OK
			 * 		- usuario existe y no tiene recetas --> 204 No Content
			 * 		- si la categor??a existe --> lista de recetas que coinciden con esa categor??a
			 * 		- si la categor??a existe y no tiene recetas --> 204 no content
			 * 		- si la categor??a no existe --> excepci??n
	 */

	@GetMapping("/recipes")
	public ResponseEntity<List<Recipe>> findRecipes(@RequestParam(required = false) Long userID, @RequestParam(required = false) Integer categoryID){
		
		List<Recipe> recipes = this.recipeService.findAllRecipes();
		ResponseEntity<List<Recipe>> re = null ;
		
		if(userID==null && categoryID==null && recipes.isEmpty()) {
			re = ResponseEntity.notFound().build(); 
		}
		else if(userID==null && categoryID==null && !recipes.isEmpty()) {
			re = ResponseEntity.ok(recipes);
		}
		else if(userID!=null && categoryID==null ) {
			
			User user = this.userService.findById(userID);
			if(user==null) {
				throw new UserNotFoundException(userID);
			}
			else if( this.recipeService.findRecipeListUser(userID).isEmpty()){
				re = ResponseEntity.noContent().build();		 
			}else {
				re = ResponseEntity.ok(this.recipeService.findRecipeListUser(userID));
			}
			
		}else if(userID==null && categoryID!=null) {
			Category category = this.categoryService.findById(categoryID);
			if(category==null) {
				throw new CategoryNotFoundException(categoryID);
			}else if(this.recipeService.findAllRecipesByCategory(categoryID).isEmpty()){
				re = ResponseEntity.noContent().build();
			}else {
				re = ResponseEntity.ok(this.recipeService.findAllRecipesByCategory(categoryID));
			}
		}
		return re;
		 
		
	}
	
	/**
	 * M??TODO que gestiona peticiones GET a /recipes/id y  busca una receta por su ID
	 * @param recipeID 
	 * @return 
	 * 			si existe la receta --> receta
	 * 			si no existe la receta --> exception RecipeNotFoundException
	 */
	@GetMapping("/recipes/{id}")
	public Recipe findRecipeByID(@PathVariable Integer id) {
		if(this.recipeService.findRecipeById(id)!=null) {
			return this.recipeService.findRecipeById(id);	
		}else {
			throw new RecipeNotFoundException(id);
		}
	}
	
	/**
	 * M??TODO que gestiona petici??n POST para a??adir una receta a la bbdd
	 * @param recipe como body de la petici??n
	 * @param id del usuario en la url
	 * @return
	 * 			si el usuario no existe --> exception UserNotFoundException()
	 * 			si la categor??a de la receta no existe --> exception CategoryNotFoundException()
	 * 			si todo es correcto --> receta
	 */
	@PostMapping("/recipes")
	public Recipe addRecipe(@RequestBody Recipe recipe) {
		
		String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		User user = this.userService.findByEmail(email);
		
		//para comprobar que no haya una receta con el mismo nombre en la bbdd
		Integer check = this.recipeService.checkRecipeName(recipe.getRecipeName());
		if(user!=null) {
			
			Integer idCategory = recipe.getCategory().getId();
			Category cat = this.categoryService.findById(idCategory);
			
			if(check!=0) {
				throw new RecipeExistException(recipe.getRecipeName());
			}
			else if(cat == null) {
				throw new CategoryNotFoundException(idCategory);
			}else {
				
				recipe.setUser(user);
				recipe.setCategory(cat);
				return this.recipeService.addRecipeBBDD(recipe);
			}
		}else {
			throw new UserNotFoundException(user.getId());
		}

	}
	
	/**
	 * M??TODO que gestiona una petici??n DELETE para eliminar una receta de la base de datos
	 * @param id
	 * @return 
	 * 			si la receta no existe --> exception RecipeNotFoundException()
	 * 			si la receta existe --> noContent 
	 */
	@DeleteMapping("/recipes/{id}")
	public ResponseEntity<?> deleteRecipe(@PathVariable Integer id){
		Recipe recipe = this.recipeService.findRecipeById(id);
		
		if(recipe==null) {
			throw new RecipeNotFoundException(id);
		}else {
			this.recipeService.deleteRecipe(recipe);
			return ResponseEntity.noContent().build();
		}
	}
	
	/**
	 * M??TODO que gestiona petici??n PUT para editar el nombre y la categor??a de una receta existente
	 * @param id
	 * @param datos a modificar de la receta
	 * @return 
	 * 			si la receta no existe --> exception RecipeNotFoundException()
	 * 			si la categor??a no existe --> CategoryNotFoundException()
	 * 			si receta y categor??a existen --> JSON con los datos que se han editado
	 */
	@PutMapping("recipes/{id}")
	public ResponseEntity<RecipeDates> editRecipe(@PathVariable Integer id, @RequestBody RecipeDates datos){
		
		Recipe recipe = this.recipeService.findRecipeById(id);
		Category category = this.categoryService.findById(datos.getCategory().getId());
		if(recipe == null) {
			throw new RecipeNotFoundException(id);
		}else if(category==null){
			throw new CategoryNotFoundException(datos.getCategory().getId());
		}else {
			this.recipeService.editRecipeDates(recipe, datos);
			this.recipeService.addRecipeBBDD(recipe);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(datos);
		
	}
	
	
	//ACCESO A RECURSOS DE SEGUNDO NIVEL
	
	/**
	 * M??TODO que gestiona una petici??n GET para obtener el listado de l??neas de ingredientes de una receta
	 * @param id de la receta
	 * @return
	 * 			si la receta no existe --> exception RecipeNotFoundException()
	 * 			si la receta existe y no tiene l??neas --> not Found
	 * 			si la receta existe y tiene l??neas --> JSON con todas las l??neas de pedido de la receta
	 */
	@GetMapping("recipes/{id}/ingredientLine")
	public ResponseEntity<List<IngredientLine>> findAllIngredientsLine(@PathVariable Integer id){
		
		Recipe recipe = this.recipeService.findRecipeById(id);
		if(recipe == null) {
			throw new RecipeNotFoundException(id);
		}else {
			List<IngredientLine> ingredients = this.recipeService.findRecipeById(id).getIngredientLine();
			
			ResponseEntity<List<IngredientLine>> re;
			
			if(ingredients.isEmpty()) {
				re = ResponseEntity.notFound().build(); 
			}else {
				re = ResponseEntity.ok(ingredients); 
			}
			
			return re;
		}
	}
		
		/**
		 * M??TODO que gestiona una petici??n GET para obtener una l??nea de ingredientes concreta de una receta
		 * @param id de la receta
		 * @return
		 * 			si la receta no existe --> exception RecipeNotFoundException()
		 * 			si la l??nea no existe --> exception IngredientLineNotFoundException()
		 * 			si la l??nea existe --> l??nea
		 */
		@GetMapping("recipes/{id}/ingredientLine/{idLine}")
		public IngredientLine getIngredientLineByID(@PathVariable Integer id, @PathVariable Integer idLine){
			
			Recipe recipe = this.recipeService.findRecipeById(id);
			if(recipe == null) {
				throw new RecipeNotFoundException(id);
			
			}else {
				Integer index = recipe.getIngredientLine().indexOf(this.ingredientLineService.findById(idLine));

				if(index==-1) {
					throw new IngredientLineNotFoundException(idLine);
				}else {
					return recipe.getIngredientLine().get(index);
				}

			}

		
		}
		/**
		 * M??TODO que gestiona una petici??n POST para a??adir una nueva l??nea de ingredientes a una receta
		 * @param id de la receta
		 * @param line
		 * @return	
		 * 			si la receta no existe --> exception RecipeNotFoundException()
		 * 			si la receta existe:
		 * 					- si el ingrediente ya se encuentra en la receta --> exception IngredientLineExist()
		 * 					- si no existe, lo a??ade a la receta
		 */
		@PostMapping("recipes/{id}/ingredientLine")
		public ResponseEntity<IngredientLine> addIngredientLine(@PathVariable Integer id, @RequestBody IngredientLine line){
			Recipe recipe = this.recipeService.findRecipeById(id);
			if(recipe == null) {
				throw new RecipeNotFoundException(id);
			
			}
			else {
				//para controlar si el ingrediente ya existe en la receta
				Integer check = this.recipeService.checkRecipeIngredient(line.getIngredient().getName());

				if (check!=0) {
					throw new IngredientLineExistException(line.getIngredient().getName());
				}else {
					return ResponseEntity.status(HttpStatus.CREATED).body(this.recipeService.addIngredientLine(line, recipe));
				}
				
			}
		}
		
		/**
		 * M??TODO que gestiona petici??n PUT para editar la cantidad de una l??nea de ingredientes
		 * @param id
		 * @param idLine
		 * @param amount
		 * @return	
		 * 			si receta no existe --> exception RecipeNotFoundException()
		 * 			si la l??nea no existe --> exception IngredientLineNotFoundException()
		 * 			si la l??nea existe --> l??nea con cantidad modificada
		 */
		@PutMapping("recipes/{id}/ingredientLine/{idLine}")
		public IngredientLine editIngredientLine(@PathVariable Integer id, @PathVariable Integer idLine, @RequestBody Integer amount) {
			Recipe recipe = this.recipeService.findRecipeById(id);
			if(recipe == null) {
				throw new RecipeNotFoundException(id);
			
			}else {
				Integer index = recipe.getIngredientLine().indexOf(this.ingredientLineService.findById(idLine));

				if(idLine-1>=recipe.getIngredientLine().size() || index==-1) {
					throw new IngredientLineNotFoundException(idLine);
				}else{
					IngredientLine ingredientLine = recipe.getIngredientLine().get(index);
					this.ingredientLineService.edit(ingredientLine, amount);
					return ingredientLine;
				}
				
			}
		}
		
		/**
		 * M??TODO que gestiona petici??n DELETE para borrar una l??nea de ingredientes de una receta
		 * @param id
		 * @param idLine
		 * @return
		 * 			si receta no existe --> exception RecipeNotFoundException()
		 * 			si la l??nea no existe --> exception IngredientLineNotFoundException()
		 * 			si la l??nea existe --> la borra
		 */
		@DeleteMapping("recipes/{id}/ingredientLine/{idLine}")
		public ResponseEntity<?> deleteIngredientLine(@PathVariable Integer id, @PathVariable Integer idLine){
			Recipe recipe = this.recipeService.findRecipeById(id);
			IngredientLine ingredientLine = this.ingredientLineService.findById(idLine);
			
			if(recipe==null) {
				throw new RecipeNotFoundException(id);
			}else if(ingredientLine== null) {
				throw new IngredientLineNotFoundException(idLine);
			}
			else {
				this.ingredientLineService.delete(ingredientLine);
				this.recipeService.addRecipeBBDD(recipe);
				return ResponseEntity.noContent().build();
			}
		}
		
	

	
	
	//GESTI??N DE EXCEPCIONES
	
	
	/**
	 * GESTI??N DE EXCEPCI??N RecipeNotFoundException
	 * @param ex
	 * @return un json con el estado, fecha, hora y mensaje de la excepci??n si la receta no se encuentra 
	 */
	@ExceptionHandler(RecipeNotFoundException.class)
	public ResponseEntity<ApiError> handleRecipeNotFound(RecipeNotFoundException ex) {
		ApiError apiError = new ApiError();
		apiError.setEstado(HttpStatus.NOT_FOUND);
		apiError.setFecha(LocalDateTime.now());
		apiError.setMensaje(ex.getMessage());
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
	}
	
	
	/**
	 * GESTI??N DE EXCEPCI??N UserNotFoundException
	 * @param ex
	 * @return un json con el estado, fecha, hora y mensaje de la excepci??n si el usuario no se encuentra
	 */
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex) {
		ApiError apiError = new ApiError();
		apiError.setEstado(HttpStatus.NOT_FOUND);
		apiError.setFecha(LocalDateTime.now());
		apiError.setMensaje(ex.getMessage());
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
	}
	
	/**
	 * GESTI??N DE EXCEPCI??N CategorynotFoundException
	 * @param ex
	 * @return un json con el estado, fecha, hora y mensaje de la excepci??n si la categor??a no se encuentra 
	 */
	@ExceptionHandler(CategoryNotFoundException.class)
	public ResponseEntity<ApiError> handleCategoryNotFound(CategoryNotFoundException ex) {
		ApiError apiError = new ApiError();
		apiError.setEstado(HttpStatus.NOT_FOUND);
		apiError.setFecha(LocalDateTime.now());
		apiError.setMensaje(ex.getMessage());
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
	}
	
	/**
	 * GESTI??N DE EXCEPCI??N IngredientLineNotFoundException
	 * @param ex
	 * @return un json con el estado, fecha, hora y mensaje de la excepci??n si la linea de ingredientes no se encuentra 
	 */
	@ExceptionHandler(IngredientLineNotFoundException.class)
	public ResponseEntity<ApiError> handleIngredientLineNotFound(IngredientLineNotFoundException ex) {
		ApiError apiError = new ApiError();
		apiError.setEstado(HttpStatus.NOT_FOUND);
		apiError.setFecha(LocalDateTime.now());
		apiError.setMensaje(ex.getMessage());
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
	}
	
	/**
	 * GESTI??N DE EXCEPCI??N IngredientLineExistException
	 * @param ex
	 * @return un json con el estado, fecha, hora y mensaje de la excepci??n si el ingredient ya existe en la receta
	 */
	@ExceptionHandler(IngredientLineExistException.class)
	public ResponseEntity<ApiError> handleIngredientLineExists(IngredientLineExistException ex) {
		ApiError apiError = new ApiError();
		apiError.setEstado(HttpStatus.CONFLICT);
		apiError.setFecha(LocalDateTime.now());
		apiError.setMensaje(ex.getMessage());
		
		return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
	}
	
	/**
	 * GESTI??N DE EXCEPCI??N RecipeExistException
	 * @param ex
	 * @return un json con el estado, fecha, hora y mensaje de la excepci??n si ya existe una receta en bbdd con ese nombre
	 */
	@ExceptionHandler(RecipeExistException.class)
	public ResponseEntity<ApiError> handleRecipeExists(RecipeExistException ex) {
		ApiError apiError = new ApiError();
		apiError.setEstado(HttpStatus.CONFLICT);
		apiError.setFecha(LocalDateTime.now());
		apiError.setMensaje(ex.getMessage());
		
		return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
	}
	
	
	/**
	 * GESTI??N DE EXCEPCI??N DE JSON MAL FORMADO
	 * @param ex
	 * @return un json con el estado, fecha, hora y mensaje de la excepci??n --> ignora la traza de la excepci??n
	 */
	@ExceptionHandler(JsonMappingException.class)
	public ResponseEntity<ApiError> handleJsonMappingException(JsonMappingException ex) {
		ApiError apiError = new ApiError();
		apiError.setEstado(HttpStatus.BAD_REQUEST);
		apiError.setFecha(LocalDateTime.now());
		apiError.setMensaje(ex.getMessage());
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//EXTRAS DE ANGULAR
	
	/**
	 * M??TODO que gestiona peticiones GET a /ver/id y  busca una receta por su ID
	 * @param recipeID 
	 * @return 
	 * 			si existe la receta --> receta
	 * 			si no existe la receta --> exception RecipeNotFoundException
	 */
	@GetMapping("/mostrar/{id}")
	public Recipe getRecipeByID(@PathVariable Integer id) {
		if(this.recipeService.findRecipeById(id)!=null) {
			return this.recipeService.findRecipeById(id);	
		}else {
			throw new RecipeNotFoundException(id);
		}
	}
	
	/**
	 * M??TODO que gestiona peticiones GET a /ver?categoryID=x y devuelve una lista de todas las recetas de esa categor??a
	 * @param categoryID
	 * @return
	 */
	@GetMapping("/mostrar")
	public ResponseEntity<List<Recipe>> getRecipesByCategory(@RequestParam Integer categoryID){
		
		List<Recipe> recipes = this.recipeService.findAllRecipes();
		ResponseEntity<List<Recipe>> re = null ;
		
		if(recipes.isEmpty()) {
			re = ResponseEntity.noContent().build(); 
		
		}else {
			Category category = this.categoryService.findById(categoryID);
			if(category==null) {
				throw new CategoryNotFoundException(categoryID);
			}else if(this.recipeService.findAllRecipesByCategory(categoryID).isEmpty()){
				re = ResponseEntity.noContent().build();
			}else {
				re = ResponseEntity.ok(this.recipeService.findAllRecipesByCategory(categoryID));
			}
		}
		return re;
		 
		
	}
	
	/**
	 * M??TODO que te devuelve el usuario de una receta
	 * @param id
	 * @return 
	 */
	@GetMapping("/mostrar/recipe/{id}")
	public User getUserByRecipe(@PathVariable Integer id) {
		if(this.recipeService.findRecipeById(id) == null) {
			throw new RecipeNotFoundException(id);
		}else {
			return this.recipeService.getUserByRecipeID(id);
		}
	}
	
	@GetMapping("/category/{id}")
	public ResponseEntity<Category> getCategoryByID(@PathVariable Integer id) {
		if(this.categoryService.findById(id) == null) {
			throw new CategoryNotFoundException(id);
		}else {
			System.out.println(this.categoryService.findById(id));
			return ResponseEntity.ok(categoryService.findById(id));
		}
	}
	

}
